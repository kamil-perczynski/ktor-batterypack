package io.github.ktor_batterypack.redis.monitoring

import io.github.ktor_batterypack.redis.LoopHandle
import io.github.ktor_batterypack.redis.RedisProps
import io.github.ktor_batterypack.redis.RedisStreamListener
import io.github.ktor_batterypack.redis.RedisStreamsBackgroundLoop
import io.github.ktor_batterypack.redis.bgloops.toXInfoResultDto
import io.lettuce.core.RedisClient
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds

private val log = LoggerFactory.getLogger(RedisStreamConsumerLagMonitorLoop::class.java)

@Singleton
class RedisStreamConsumerLagMonitorLoop(
    private val redisClient: RedisClient,
    private val metrics: RedisStreamMetrics,
    @Provided private val redisProps: RedisProps,
) : RedisStreamsBackgroundLoop {

    override fun start(
        fetcherId: String,
        listeners: Map<String, RedisStreamListener>,
        consumerGroup: String,
    ): LoopHandle {
        val connection = redisClient.connect()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineName("LagMonitor"))

        val lagCheckIntervalMs = redisProps.fetcher.lagCheckIntervalMs
        val streams = listeners.keys.toList()

        val job = scope.launch {
            while (isActive) {
                val allConsumers = mutableListOf<StreamConsumerMetrics>()
                for (stream in streams) {
                    try {
                        val raw = connection.async()
                            .xinfoConsumers(stream, consumerGroup)
                            .await() as List<*>

                        val consumersInfo = toXInfoResultDto(raw)

                        for (consumerInfo in consumersInfo) {
                            allConsumers.add(
                                StreamConsumerMetrics(
                                    stream = stream,
                                    consumerGroup = consumerGroup,
                                    consumerId = consumerInfo.name,
                                    pending = consumerInfo.pending,
                                    idle = consumerInfo.idle,
                                    inactive = consumerInfo.inactive
                                )
                            )
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        log.debug("Failed to read consumers for stream {}: {}", stream, e.message)
                    }
                }
                metrics.recordStreamInfo(allConsumers)
                delay(lagCheckIntervalMs.milliseconds)
            }
        }

        return object : LoopHandle {
            override fun close() {
                runBlocking {
                    scope.cancel()
                    scope.coroutineContext.job.join()
                }
                if (connection.isOpen) {
                    log.debug("Closing Redis connection for consumer lag monitor loop")
                    connection.close()
                }
            }
        }
    }
}
