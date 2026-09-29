package io.github.ktor_batterypack.redis.bgloops

import io.github.ktor_batterypack.redis.LoopHandle
import io.github.ktor_batterypack.redis.RedisFacade
import io.github.ktor_batterypack.redis.RedisConnectionFacade
import io.github.ktor_batterypack.redis.RedisProps
import io.github.ktor_batterypack.redis.RedisStreamListener
import io.github.ktor_batterypack.redis.RedisStreamsBackgroundLoop
import io.github.ktor_batterypack.redis.monitoring.RedisStreamMetrics
import io.lettuce.core.Consumer
import io.lettuce.core.XAutoClaimArgs
import io.lettuce.core.models.stream.ClaimedMessages
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds

private val log = LoggerFactory.getLogger(RedisStreamAutoclaimLoop::class.java)

/**
 * Background loop that periodically runs XAUTOCLAIM to claim pending messages that have been idle for too long.
 * This helps ensure that messages are not stuck indefinitely if a consumer crashes while processing them.
 */
@Singleton
class RedisStreamAutoclaimLoop(
    private val connectionFacade: RedisFacade,
    private val messageProcessor: StreamMessageProcessor,
    private val metrics: RedisStreamMetrics,
    @Provided private val redisProps: RedisProps,
) : RedisStreamsBackgroundLoop {

    override fun start(
        fetcherId: String,
        listeners: Map<String, RedisStreamListener>,
        consumerGroup: String,
    ): LoopHandle {
        val redis = connectionFacade.connect()
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
                            redis = redis
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
                if (redis.isOpen) {
                    log.debug("Closing Redis connection for autoclaim loop")
                    redis.close()
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
        redis: RedisConnectionFacade,
    ) {
        val args = XAutoClaimArgs<String>()
            .consumer(consumer)
            .minIdleTime(autoclaimMinIdleMs)
            .startId("0-0")
            .count(autoclaimCount)

        val result: ClaimedMessages<String, String> = redis
            .streamAsync
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
            redis = redis
        )
    }
}
