package io.github.kperczynski.infra.florin

import io.github.kperczynski.libs.di.InitCallback
import io.github.kperczynski.libs.redis.RedisModule
import io.github.kperczynski.libs.redis.RedisProps
import io.github.kperczynski.libs.redis.RedisStreamFetcher
import io.github.kperczynski.libs.redis.RedisStreamListener
import io.github.kperczynski.libs.redis.RedisStreamListenerGroups.Companion.TEST_GROUP
import io.github.kperczynski.libs.redis.RedisStreamMetrics
import io.lettuce.core.RedisClient
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton

@Configuration
@Module(includes = [RedisModule::class])
class TestRedisModule {

    @Singleton
    fun testRedisStreamMetrics(): RedisStreamMetrics {
        return RedisStreamMetrics(SimpleMeterRegistry())
    }

    @Singleton(binds = [InitCallback::class, AutoCloseable::class])
    @Named("testRedisFetcher")
    fun testRedisFetcher(
        redisClient: RedisClient,
        redisProps: RedisProps,
        listeners: List<RedisStreamListener>,
        metrics: RedisStreamMetrics
    ): RedisStreamFetcher {
        return RedisStreamFetcher(
            fetcherId = "Test-1",
            redisClient = redisClient,
            listeners = listeners.filter { it.group() == TEST_GROUP },
            consumerGroup = "test",
            fetchingTimeout = redisProps.fetcher.fetchingTimeout,
            fetchingCount = redisProps.fetcher.fetchingCount,
            autoclaimIntervalMs = redisProps.fetcher.autoclaimIntervalMs,
            autoclaimMinIdleMs = redisProps.fetcher.autoclaimMinIdleMs,
            autoclaimCount = redisProps.fetcher.autoclaimCount,
            lagCheckIntervalMs = redisProps.fetcher.lagCheckIntervalMs,
            metrics = metrics
        )
    }

}
