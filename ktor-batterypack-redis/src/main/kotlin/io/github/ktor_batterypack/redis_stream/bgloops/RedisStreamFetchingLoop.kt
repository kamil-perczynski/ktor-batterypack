package io.github.ktor_batterypack.redis_stream.bgloops

import io.github.ktor_batterypack.redis_stream.LoopHandle
import io.github.ktor_batterypack.redis.RedisProps
import io.github.ktor_batterypack.redis_stream.RedisStreamListener
import io.github.ktor_batterypack.redis_stream.RedisStreamsBackgroundLoop
import io.github.ktor_batterypack.redis_stream.monitoring.RedisStreamMetrics
import io.lettuce.core.Consumer
import io.lettuce.core.RedisBusyException
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisCommandExecutionException
import io.lettuce.core.XGroupCreateArgs
import io.lettuce.core.XReadArgs
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds

private val log = LoggerFactory.getLogger(RedisStreamFetchingLoop::class.java)

@Singleton
class RedisStreamFetchingLoop(
    private val redisClient: RedisClient,
    private val messageProcessor: StreamMessageProcessor,
    private val metrics: RedisStreamMetrics,
    @Provided private val redisProps: RedisProps,
) : RedisStreamsBackgroundLoop {

    override fun start(
        fetcherId: String,
        listeners: Map<String, RedisStreamListener>,
        consumerGroup: String,
    ): LoopHandle {
        val connection = ReconnectingRedisConnection(redisClient, "stream-fetching", metrics)
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineName("StreamFetching"))

        val fetchingTimeout = redisProps.streams.fetcher.fetchingTimeout
        val fetchingCount = redisProps.streams.fetcher.fetchingCount
        val streams = listeners.keys.toList()
        val consumer = Consumer.from(consumerGroup, fetcherId)

        val job = scope.launch {
            val offsets = streams.map { XReadArgs.StreamOffset.from(it, ">") }.toTypedArray()
            var consumerGroupMissing = false

            while (isActive) {
                try {
                    connection.execute { conn ->
                        if (consumerGroupMissing) {
                            createConsumerGroups(conn, streams, consumerGroup)
                            consumerGroupMissing = false
                        }

                        val messages = conn
                            .async()
                            .xreadgroup(
                                consumer,
                                XReadArgs.Builder.block(fetchingTimeout).count(fetchingCount),
                                *offsets
                            )
                            .await()

                        messageProcessor.processMessages(
                            messages = messages,
                            listeners = listeners,
                            consumerGroup = consumerGroup,
                            connection = conn
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    consumerGroupMissing = isMissingConsumerGroup(e)
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

        return object : LoopHandle {
            override fun close() {
                runBlocking {
                    scope.cancel()
                    scope.coroutineContext.job.join()
                }
                connection.close()
            }
        }
    }

    private fun isMissingConsumerGroup(e: Exception): Boolean {
        return e is RedisCommandExecutionException && e.message?.startsWith("NOGROUP") == true
    }

    private suspend fun createConsumerGroups(
        connection: StatefulRedisConnection<String, String>,
        streams: List<String>,
        consumerGroup: String,
    ) {
        log.warn("Consumer group {} is missing, recreating it for streams: {}", consumerGroup, streams)

        for (stream in streams) {
            try {
                connection
                    .async()
                    .xgroupCreate(
                        XReadArgs.StreamOffset.from(stream, "0"),
                        consumerGroup,
                        XGroupCreateArgs().mkstream(true)
                    )
                    .await()
            } catch (e: RedisBusyException) {
                log.debug("Consumer group {} already exists for stream {}", consumerGroup, stream)
            }
        }
    }
}
