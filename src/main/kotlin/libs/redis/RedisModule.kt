package io.github.kperczynski.libs.redis

import io.github.ktor_batterypack.core.di.InitCallback
import io.github.kperczynski.libs.health.ReadinessCheck
import io.github.kperczynski.libs.redis.RedisStreamListenerGroups.Companion.MAIN_GROUP
import io.github.kperczynski.libs.redis.monitoring.RedisReadinessCheck
import io.github.kperczynski.libs.redis.monitoring.RedisStreamMetrics
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.metrics.MicrometerCommandLatencyRecorder
import io.lettuce.core.metrics.MicrometerOptions
import io.lettuce.core.resource.ClientResources
import io.micrometer.core.instrument.MeterRegistry
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton

@Module
@Configuration
class RedisModule {

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
        redisProps: RedisProps,
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
    fun redisClient(redisProps: RedisProps, meterRegistry: MeterRegistry): RedisClient {
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