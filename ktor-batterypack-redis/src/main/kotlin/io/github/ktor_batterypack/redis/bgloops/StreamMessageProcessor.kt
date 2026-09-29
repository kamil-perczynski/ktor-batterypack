package io.github.ktor_batterypack.redis.bgloops

import io.github.ktor_batterypack.redis.RedisConnectionFacade
import io.github.ktor_batterypack.redis.RedisStreamListener
import io.github.ktor_batterypack.redis.monitoring.RedisStreamMetrics
import io.lettuce.core.StreamMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.future.await
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory
import kotlin.collections.iterator

private val log = LoggerFactory.getLogger(StreamMessageProcessor::class.java)

@Singleton
class StreamMessageProcessor(private val metrics: RedisStreamMetrics) {

    suspend fun processMessages(
        messages: List<StreamMessage<String, String>>,
        listeners: Map<String, RedisStreamListener>,
        consumerGroup: String,
        redis: RedisConnectionFacade,
    ) {
        val grouped = messages.groupBy { it.stream }

        coroutineScope {
            for ((_, msgs) in grouped) {
                launch(CoroutineName("Listener")) {
                    for (message in msgs) {
                        processMessage(message, listeners, consumerGroup, redis)
                    }
                }
            }
        }
    }

    private suspend fun processMessage(
        message: StreamMessage<String, String>,
        listeners: Map<String, RedisStreamListener>,
        consumerGroup: String,
        redis: RedisConnectionFacade,
    ) {
        log.debug("Received redis stream message: {}", message)
        val payload = message.body["_p"] ?: ""
        val headers = message.body.filterKeys { it != "_p" }

        val startNanos = System.nanoTime()
        try {
            listeners[message.stream]?.onMessage(payload, headers)
            val durationNanos = System.nanoTime() - startNanos
            metrics.recordListenerDuration(message.stream, consumerGroup, "n/a", durationNanos)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val durationNanos = System.nanoTime() - startNanos
            metrics.recordListenerDuration(
                message.stream,
                consumerGroup,
                e::class.java.simpleName,
                durationNanos
            )
            log.error(
                "Error processing message from stream {} with id {}: {}",
                message.stream,
                message.id,
                e.message,
                e
            )
        } finally {
            withContext(Dispatchers.IO + NonCancellable) {
                redis
                    .streamAsync
                    .xack(message.stream, consumerGroup, message.id)
                    .await()
            }
        }
    }
}
