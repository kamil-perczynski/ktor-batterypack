package io.github.ktor_batterypack.redis

import io.github.ktor_batterypack.core.health.ReadinessCheck
import io.github.ktor_batterypack.redis.cluster.ClusterRedisFacade
import io.github.ktor_batterypack.redis.monitoring.RedisReadinessCheck
import io.github.ktor_batterypack.redis.monitoring.RedisStreamMetrics
import io.github.ktor_batterypack.redis.standalone.StandaloneRedisFacade
import io.lettuce.core.RedisClient
import io.lettuce.core.cluster.RedisClusterClient
import io.lettuce.core.metrics.MicrometerCommandLatencyRecorder
import io.lettuce.core.metrics.MicrometerOptions
import io.lettuce.core.resource.ClientResources
import io.micrometer.core.instrument.MeterRegistry
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Singleton

@Module
@ComponentScan("io.github.ktor_batterypack.redis")
class KtorBatterypackRedisModule {

    @Singleton(binds = [ReadinessCheck::class])
    fun redisCheck(connectionFacade: RedisFacade): RedisReadinessCheck {
        return RedisReadinessCheck(connectionFacade)
    }

    @Singleton
    fun redisStreamMetrics(meterRegistry: MeterRegistry): RedisStreamMetrics {
        return RedisStreamMetrics(meterRegistry)
    }

    @Singleton(binds = [AutoCloseable::class])
    fun redisConnectionFacade(
        @Provided redisProps: RedisProps,
        meterRegistry: MeterRegistry
    ): RedisFacade {
        val options = MicrometerOptions.builder()
            .localDistinction(false)
            .build()

        val resources = ClientResources.builder()
            .commandLatencyRecorder(MicrometerCommandLatencyRecorder(meterRegistry, options))
            .build()

        return when (redisProps.mode) {
            RedisMode.STANDALONE ->
                StandaloneRedisFacade(RedisClient.create(resources, redisProps.url))

            RedisMode.CLUSTER ->
                ClusterRedisFacade(RedisClusterClient.create(resources, redisProps.url))
        }
    }

}
