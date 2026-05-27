package io.github.kperczynski.libs.redis.bgloops

import io.github.kperczynski.libs.redis.LoopHandle
import io.github.kperczynski.libs.redis.RedisProps
import io.github.kperczynski.libs.redis.RedisStreamListener
import io.github.kperczynski.libs.redis.RedisStreamsBackgroundLoop
import io.github.kperczynski.libs.redis.monitoring.RedisStreamMetrics
import io.lettuce.core.Consumer
import io.lettuce.core.RedisClient
import io.lettuce.core.XAutoClaimArgs
import io.lettuce.core.models.stream.ClaimedMessages
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds

private val log = LoggerFactory.getLogger(RedisStreamAutoclaimLoop::class.java)

@Singleton
class RedisStreamAutoclaimLoop(
    private val redisClient: RedisClient,
    private val messageProcessor: StreamMessageProcessor,
    private val metrics: RedisStreamMetrics,
    private val redisProps: RedisProps,
) : RedisStreamsBackgroundLoop {

    override fun start(
        fetcherId: String,
        listeners: Map<String, RedisStreamListener>,
        consumerGroup: String,
    ): LoopHandle {
        val connection = redisClient.connect()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineName("Autoclaim"))

        val autoclaimIntervalMs = redisProps.fetcher.autoclaimIntervalMs
        val autoclaimMinIdleMs = redisProps.fetcher.autoclaimMinIdleMs
        val autoclaimCount = redisProps.fetcher.autoclaimCount
        val streams = listeners.keys.toList()
        val consumer = Consumer.from(consumerGroup, fetcherId)

        val job = scope.launch {
            while (isActive) {
                delay(autoclaimIntervalMs.milliseconds)

                for (stream in streams) {
                    try {
                        autoclaimStream(
                            stream = stream,
                            consumer = consumer,
                            consumerGroup = consumerGroup,
                            autoclaimMinIdleMs = autoclaimMinIdleMs,
                            autoclaimCount = autoclaimCount,
                            listeners = listeners,
                            connection = connection
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        log.error("Redis autoclaim failed for stream {}: {}", stream, e.message, e)
                    }
                }
            }
        }

        return object : LoopHandle {
            override fun close() {
                runBlocking {
                    scope.cancel()
                    scope.coroutineContext.job.join()
                }
                if (connection.isOpen) {
                    log.debug("Closing Redis connection for autoclaim loop")
                    connection.close()
                }
            }
        }
    }

    private suspend fun autoclaimStream(
        stream: String,
        consumer: Consumer<String>,
        consumerGroup: String,
        autoclaimMinIdleMs: Long,
        autoclaimCount: Long,
        listeners: Map<String, RedisStreamListener>,
        connection: io.lettuce.core.api.StatefulRedisConnection<String, String>,
    ) {
        val args = XAutoClaimArgs<String>()
            .consumer(consumer)
            .minIdleTime(autoclaimMinIdleMs)
            .startId("0-0")
            .count(autoclaimCount)

        val result: ClaimedMessages<String, String> = connection
            .async()
            .xautoclaim(stream, args)
            .await()

        val messages = result.messages
        if (messages.isEmpty()) {
            return
        }

        log.info("Reclaiming {} pending message(s) from stream {}", messages.size, stream)
        metrics.recordAutoclaimReclaimed(stream, messages.size)

        messageProcessor.processMessages(
            messages = messages,
            listeners = listeners,
            consumerGroup = consumerGroup,
            connection = connection
        )
    }
}
