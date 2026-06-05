package io.github.ktor_batterypack.redis

import io.github.ktor_batterypack.redis.monitoring.RedisReadinessCheck
import io.github.ktor_batterypack.redis.monitoring.RedisStreamMetrics
import io.github.ktor_batterypack.redis.RedisStreamListenerGroups.Companion.MAIN_GROUP
import io.github.ktor_batterypack.core.health.ReadinessCheck
import io.github.ktor_batterypack.core.di.InitCallback
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.metrics.MicrometerCommandLatencyRecorder
import io.lettuce.core.metrics.MicrometerOptions
import io.lettuce.core.resource.ClientResources
import io.micrometer.core.instrument.MeterRegistry
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Singleton

@Module
@Configuration
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

    @Singleton(binds = [InitCallback::class, AutoCloseable::class])
    @Named("redisFetcher")
    fun redisFetcher(
        redisClient: RedisClient,
        @Provided redisProps: RedisProps,
        listeners: List<RedisStreamListener>,
        loops: List<RedisStreamsBackgroundLoop>,
    ): RedisStreamFetcher {
        return RedisStreamFetcher(
            consumerId = redisProps.fetcher.consumerPrefix + System.currentTimeMillis().toHexString(),
            redisClient = redisClient,
            consumerGroup = redisProps.fetcher.consumerGroup,
            autoclaimMinIdleMs = redisProps.fetcher.autoclaimMinIdleMs,
            loops = loops,
            listeners = listeners.filter { it.group() == MAIN_GROUP },
        )
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
