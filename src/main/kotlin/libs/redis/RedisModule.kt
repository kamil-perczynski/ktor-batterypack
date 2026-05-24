package io.github.kperczynski.libs.redis

import io.github.kperczynski.libs.di.InitCallback
import io.github.kperczynski.libs.health.ReadinessCheck
import io.github.kperczynski.libs.redis.RedisStreamListenerGroups.Companion.MAIN_GROUP
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

    @Singleton(binds = [InitCallback::class, AutoCloseable::class])
    @Named("redisFetcher")
    fun redisFetcher(
        redisClient: RedisClient,
        redisProps: RedisProps,
        listeners: List<RedisStreamListener>
    ): RedisStreamFetcher {
        return RedisStreamFetcher(
            fetcherId = redisProps.fetcher.consumerPrefix + System.currentTimeMillis().toHexString(),
            redisClient = redisClient,
            listeners = listeners.filter { it.group() == MAIN_GROUP },
            consumerGroup = redisProps.fetcher.consumerGroup,
            fetchingTimeout = redisProps.fetcher.fetchingTimeout,
            fetchingCount = redisProps.fetcher.fetchingCount,
            autoclaimIntervalMs = redisProps.fetcher.autoclaimIntervalMs,
            autoclaimMinIdleMs = redisProps.fetcher.autoclaimMinIdleMs,
            autoclaimCount = redisProps.fetcher.autoclaimCount
        )
    }

    @Singleton(binds = [AutoCloseable::class])
    fun redisClient(redisProps: RedisProps, meterRegistry: MeterRegistry): RedisClient {
        val options = MicrometerOptions.builder()
            .targetPercentiles(doubleArrayOf(0.5, 0.95, 0.99))
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