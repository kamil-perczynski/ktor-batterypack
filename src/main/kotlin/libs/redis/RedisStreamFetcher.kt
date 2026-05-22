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
    dispatcher: CoroutineDispatcher,
) : InitCallback, AutoCloseable {

    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    private lateinit var consumerConnection: StatefulRedisConnection<String, String>
    private lateinit var autoclaimConnection: StatefulRedisConnection<String, String>

    private val listenersIdx by lazy { listeners.associateBy { it.stream() } }

    override fun onInit() {
        this.consumerConnection = redisClient.connect()
        this.autoclaimConnection = redisClient.connect()

        val streams = listeners.map { it.stream() }.distinct()

        createConsumerGroups(streams)
        createFetchingLoopJob(scope, streams)
        createAutoclaimJob(scope, streams)
    }

    private fun createFetchingLoopJob(scope: CoroutineScope, streams: List<String>): Job {
        return scope.launch(CoroutineName("RedisStreamFetcher-$fetcherId")) {
            val consumer = Consumer.from(consumerGroup, fetcherId)
            val offsets = streams.map { XReadArgs.StreamOffset.from(it, ">") }.toTypedArray()

            while (isActive) {
                val messages = consumerConnection
                    .async()
                    .xreadgroup(
                        consumer,
                        XReadArgs.Builder.block(fetchingTimeout).count(fetchingCount),
                        *offsets
                    )
                    .await()

                for (message in messages) {
                    processMessage(message, consumerConnection)
                }
            }
        }
    }

    private fun createAutoclaimJob(scope: CoroutineScope, streams: List<String>): Job {
        return scope.launch(CoroutineName("RedisStreamAutoclaim-$fetcherId")) {
            val consumer = Consumer.from(consumerGroup, fetcherId)

            while (isActive) {
                delay(autoclaimIntervalMs.milliseconds)

                for (stream in streams) {
                    if (!isActive) {
                        break
                    }
                    autoclaimStream(stream, consumer)
                }
            }
        }
    }

    private suspend fun autoclaimStream(stream: String, consumer: Consumer<String>) {
        var startId = "0-0"

        val args = XAutoClaimArgs<String>()
            .consumer(consumer)
            .minIdleTime(autoclaimMinIdleMs)
            .startId(startId)
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

        for (message in messages) {
            processMessage(message, autoclaimConnection)
        }

        startId = result.id
    }

    private suspend fun processMessage(
        message: StreamMessage<String, String>,
        connection: StatefulRedisConnection<String, String>,
    ) {
        log.debug("Received redis stream message: {}", message)
        val payload = message.body["_p"] ?: ""
        val headers = message.body.filterKeys { it != "_p" }

        try {
            listenersIdx[message.stream]?.onMessage(payload, headers)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.error(
                "Error processing message from stream {} with id {}: {}",
                message.stream,
                message.id,
                e.message,
                e
            )
        } finally {
            if (Math.random() > .5) {
                connection
                    .async()
                    .xack(message.stream, consumerGroup, message.id)
                    .await()
            }
        }
    }

    private fun createConsumerGroups(streams: List<String>) {
        runBlocking {
            log.info(
                "Registering {} redis stream listener(s) in group: {}, streams: {}",
                listeners.size,
                consumerGroup,
                streams
            )

            for (streamKey in streams) {
                try {
                    consumerConnection.async()
                        .xgroupCreate(
                            XReadArgs.StreamOffset.from(streamKey, "0"),
                            consumerGroup,
                            XGroupCreateArgs().mkstream(true)
                        )
                        .await()
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
    }

    override fun close() {
        log.info("Closing RedisStreamFetcher: {}", fetcherId)
        runBlocking {
            scope.cancel()
            consumerConnection.close()
            autoclaimConnection.close()
        }
    }

}