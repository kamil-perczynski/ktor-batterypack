package io.github.kperczynski.libs.redis

import io.github.kperczynski.libs.di.InitCallback
import io.lettuce.core.Consumer
import io.lettuce.core.RedisBusyException
import io.lettuce.core.RedisClient
import io.lettuce.core.StreamMessage
import io.lettuce.core.XAutoClaimArgs
import io.lettuce.core.XGroupCreateArgs
import io.lettuce.core.XReadArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.models.stream.ClaimedMessages
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.slf4j.LoggerFactory
import java.lang.AutoCloseable
import kotlin.time.Duration.Companion.milliseconds

private val log = LoggerFactory.getLogger(RedisStreamFetcher::class.java)

class RedisStreamFetcher(
    private val fetcherId: String,
    private val redisClient: RedisClient,
    private val listeners: List<RedisStreamListener>,
    private val consumerGroup: String,
    private val fetchingTimeout: Long,
    private val fetchingCount: Long,
    private val autoclaimIntervalMs: Long,
    private val autoclaimMinIdleMs: Long,
    private val autoclaimCount: Long,
    private val lagCheckIntervalMs: Long,
    private val metrics: RedisStreamMetrics,
) : InitCallback, AutoCloseable {

    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineName("Redis"))

    private lateinit var consumerConnection: StatefulRedisConnection<String, String>
    private lateinit var autoclaimConnection: StatefulRedisConnection<String, String>
    private lateinit var lagConnection: StatefulRedisConnection<String, String>

    private val listenersIdx by lazy { listeners.associateBy { it.stream() } }

    override fun onInit() {
        this.consumerConnection = redisClient.connect()
        this.autoclaimConnection = redisClient.connect()
        this.lagConnection = redisClient.connect()

        val streams = listeners.map { it.stream() }.distinct()

        cleanupInactiveConsumers(streams)
        createConsumerGroups(streams)

        createFetchingLoopJob(streams)
        createAutoclaimJob(streams)
        createLagMonitorJob(streams)
    }

    private fun createFetchingLoopJob(streams: List<String>): Job {
        return ioScope.launch {
            val consumer = Consumer.from(consumerGroup, fetcherId)
            val offsets = streams.map { XReadArgs.StreamOffset.from(it, ">") }.toTypedArray()

            while (isActive) {
                try {
                    val messages = consumerConnection
                        .async()
                        .xreadgroup(
                            consumer,
                            XReadArgs.Builder.block(fetchingTimeout).count(fetchingCount),
                            *offsets
                        )
                        .await()

                    processMessages(consumerConnection, messages)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.error(
                        "Redis xreadgroup failed, retrying after {}ms: {}",
                        fetchingTimeout,
                        e.message,
                        e
                    )
                    delay(fetchingTimeout.milliseconds)
                }
            }
        }
    }

    private fun createAutoclaimJob(streams: List<String>): Job {
        return ioScope.launch(CoroutineName("Autoclaim")) {
            val consumer = Consumer.from(consumerGroup, fetcherId)

            while (isActive) {
                delay(autoclaimIntervalMs.milliseconds)

                for (stream in streams) {
                    try {
                        autoclaimStream(stream, consumer)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        log.error("Redis autoclaim failed for stream {}: {}", stream, e.message, e)
                    }
                }
            }
        }
    }

    private fun createLagMonitorJob(streams: List<String>): Job {
        return ioScope.launch(CoroutineName("LagMonitor")) {
            while (isActive) {
                delay(lagCheckIntervalMs.milliseconds)
                for (stream in streams) {
                    try {
                        checkLag(stream)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        log.debug("Failed to check lag for stream {}: {}", stream, e.message)
                    }
                }
            }
        }
    }

    private suspend fun checkLag(stream: String) {
        val streamRaw = lagConnection.async().xinfoStream(stream).await() as List<*>
        val lastGeneratedId = parseXInfoStreamLastGeneratedId(streamRaw) ?: return

        val groupsRaw = lagConnection.async().xinfoGroups(stream).await() as List<*>
        val groups = toXInfoGroupResultDto(groupsRaw)
        val group = groups.find { it.name == consumerGroup } ?: return

        val lagMs = idToTimestamp(lastGeneratedId) - idToTimestamp(group.lastDeliveredId)
        metrics.recordLag(stream, consumerGroup, lagMs.coerceAtLeast(0), group.pending)
    }

    private fun idToTimestamp(id: String): Long {
        return id.substringBefore("-").toLongOrNull() ?: 0L
    }

    private suspend fun autoclaimStream(stream: String, consumer: Consumer<String>) {
        val args = XAutoClaimArgs<String>()
            .consumer(consumer)
            .minIdleTime(autoclaimMinIdleMs)
            .startId("0-0")
            .count(autoclaimCount)

        val result: ClaimedMessages<String, String> = autoclaimConnection
            .async()
            .xautoclaim(stream, args)
            .await()

        val messages = result.messages
        if (messages.isEmpty()) {
            return
        }

        log.info("Reclaiming {} pending message(s) from stream {}", messages.size, stream)
        metrics.recordAutoclaimReclaimed(stream, messages.size)

        processMessages(autoclaimConnection, messages)
    }

    private suspend fun processMessages(
        connection: StatefulRedisConnection<String, String>,
        messages: List<StreamMessage<String, String>>
    ) {
        val grouped = messages.groupBy { it.stream }
        coroutineScope {
            for ((_, msgs) in grouped) {
                launch(CoroutineName("Listener")) {
                    for (message in msgs) {
                        processMessage(message, connection)
                    }
                }
            }
        }
    }

    private suspend fun processMessage(
        message: StreamMessage<String, String>,
        connection: StatefulRedisConnection<String, String>,
    ) {
        log.debug("Received redis stream message: {}", message)
        val payload = message.body["_p"] ?: ""
        val headers = message.body.filterKeys { it != "_p" }

        val startNanos = System.nanoTime()
        try {
            listenersIdx[message.stream]?.onMessage(payload, headers)
            val durationNanos = System.nanoTime() - startNanos
            metrics.recordListenerDuration(message.stream, consumerGroup, "success", durationNanos)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val durationNanos = System.nanoTime() - startNanos
            metrics.recordListenerDuration(message.stream, consumerGroup, e::class.java.simpleName, durationNanos)
            log.error(
                "Error processing message from stream {} with id {}: {}",
                message.stream,
                message.id,
                e.message,
                e
            )
        } finally {
            withContext(Dispatchers.IO + NonCancellable) {
                connection
                    .async()
                    .xack(message.stream, consumerGroup, message.id)
                    .await()
            }
        }
    }

    private fun cleanupInactiveConsumers(streams: List<String>) {
        for (stream in streams) {
            try {
                val raw = consumerConnection
                    .sync()
                    .xinfoConsumers(stream, consumerGroup)

                val consumers = toXInfoResultDto(raw)

                for (info in consumers) {
                    if (info.name != fetcherId && info.pending == 0L && info.idle > autoclaimMinIdleMs) {
                        consumerConnection
                            .sync()
                            .xgroupDelconsumer(stream, Consumer.from(consumerGroup, info.name))
                        log.info("Removed inactive consumer {} from stream {}", info.name, stream)
                    }
                }
            } catch (e: Exception) {
                log.warn(
                    "Failed to cleanup inactive consumers for stream {}: {}",
                    stream,
                    e.message,
                    e
                )
            }
        }
    }

    private fun createConsumerGroups(streams: List<String>) {
        log.info(
            "Registering {} redis stream listener(s) in group: {}, streams: {}",
            listeners.size,
            consumerGroup,
            streams
        )

        for (streamKey in streams) {
            try {
                consumerConnection.sync()
                    .xgroupCreate(
                        XReadArgs.StreamOffset.from(streamKey, "0"),
                        consumerGroup,
                        XGroupCreateArgs().mkstream(true)
                    )
            } catch (e: RedisBusyException) {
                log.warn(
                    "Consumer group {} already exists for stream {}, skipping group creation",
                    consumerGroup,
                    streamKey
                )
                log.trace("Existing consumer group error details", e)
            }
        }
    }

    override fun close() {
        log.info("Closing RedisStreamFetcher: {}", fetcherId)
        runBlocking {
            ioScope.cancel()
            ioScope.coroutineContext.job.join()
            consumerConnection.close()
            autoclaimConnection.close()
            lagConnection.close()
        }
    }
}
