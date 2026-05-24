package io.github.kperczynski.libs.redis.monitoring

import io.github.kperczynski.libs.redis.RedisProps
import io.github.kperczynski.libs.redis.RedisStreamListener
import io.github.kperczynski.libs.redis.RedisStreamsBackgroundLoop
import io.github.kperczynski.libs.redis.bgloops.toXInfoResultDto
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.future.await
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds

private val log = LoggerFactory.getLogger(RedisStreamConsumerLagMonitorLoop::class.java)

@Singleton
class RedisStreamConsumerLagMonitorLoop(
    private val redisClient: RedisClient,
    private val metrics: RedisStreamMetrics,
    private val redisProps: RedisProps,
) : RedisStreamsBackgroundLoop {

    private lateinit var connection: StatefulRedisConnection<String, String>
    private lateinit var scope: CoroutineScope
    private lateinit var job: Job

    override fun init() {
        connection = redisClient.connect()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineName("LagMonitor"))
    }

    override fun close() {
        runBlocking {
            scope.cancel()
            scope.coroutineContext.job.join()
        }
        connection.close()
    }

    override fun start(
        fetcherId: String,
        listeners: Map<String, RedisStreamListener>,
        consumerGroup: String,
    ) {
        val lagCheckIntervalMs = redisProps.fetcher.lagCheckIntervalMs
        val streams = listeners.keys.toList()

        job = scope.launch {
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
    }
}
