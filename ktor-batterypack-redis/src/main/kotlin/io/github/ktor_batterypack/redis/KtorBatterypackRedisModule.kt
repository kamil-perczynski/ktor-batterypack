package io.github.ktor_batterypack.redis

import io.github.ktor_batterypack.core.health.ReadinessCheck
import io.github.ktor_batterypack.redis.monitoring.RedisReadinessCheck
import io.github.ktor_batterypack.redis.monitoring.RedisStreamMetrics
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
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
    fun redisCheck(redisClient: RedisClient): RedisReadinessCheck {
        return RedisReadinessCheck(redisClient)
    }

    @Singleton
    fun redisStreamMetrics(meterRegistry: MeterRegistry): RedisStreamMetrics {
        return RedisStreamMetrics(meterRegistry)
    }

    @Singleton(binds = [AutoCloseable::class])
    fun redisClient(@Provided redisProps: RedisProps, meterRegistry: MeterRegistry): RedisClient {
        val options = MicrometerOptions.builder()
            .localDistinction(false)
            .build()

        val resources = ClientResources.builder()
            .commandLatencyRecorder(MicrometerCommandLatencyRecorder(meterRegistry, options))
            .build()

        return RedisClient.create(resources, redisProps.url)
    }

    @Singleton
    fun statefulRedisConnection(redisClient: RedisClient): StatefulRedisConnection<String, String> {
        return redisClient.connect()
    }

}
