package io.github.kperczynski.infra.florin

import io.github.kperczynski.libs.di.InitCallback
import io.github.kperczynski.libs.redis.*
import io.github.kperczynski.libs.redis.RedisStreamListenerGroups.Companion.TEST_GROUP
import io.github.kperczynski.libs.redis.RedisStreamsBackgroundLoop
import io.github.kperczynski.libs.redis.monitoring.RedisStreamMetrics
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
        loops: List<RedisStreamsBackgroundLoop>,
    ): RedisStreamFetcher {
        return RedisStreamFetcher(
            consumerId = "Test",
            redisClient = redisClient,
            listeners = listeners.filter { it.group() == TEST_GROUP },
            consumerGroup = "test",
            autoclaimMinIdleMs = redisProps.fetcher.autoclaimMinIdleMs,
            loops = loops,
        )
    }

}
