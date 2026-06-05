package io.github.ktor_batterypack.redis.testing

import io.github.ktor_batterypack.core.di.InitCallback
import io.github.ktor_batterypack.redis.KtorBatterypackRedisModule
import io.github.ktor_batterypack.redis.RedisProps
import io.github.ktor_batterypack.redis.RedisStreamFetcher
import io.github.ktor_batterypack.redis.RedisStreamListener
import io.github.ktor_batterypack.redis.RedisStreamListenerGroups.Companion.TEST_GROUP
import io.github.ktor_batterypack.redis.RedisStreamsBackgroundLoop
import io.github.ktor_batterypack.redis.monitoring.RedisStreamMetrics
import io.lettuce.core.RedisClient
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Singleton

@Configuration
@Module(includes = [KtorBatterypackRedisModule::class])
class TestRedisModule {

    @Singleton
    fun testRedisStreamMetrics(): RedisStreamMetrics {
        return RedisStreamMetrics(SimpleMeterRegistry())
    }

    @Singleton(binds = [InitCallback::class, AutoCloseable::class])
    @Named("testRedisFetcher")
    fun testRedisFetcher(
        redisClient: RedisClient,
        @Provided redisProps: RedisProps,
        listeners: List<RedisStreamListener>,
        loops: List<RedisStreamsBackgroundLoop>,
    ): RedisStreamFetcher {
        return RedisStreamFetcher(
            consumerId = "Test",
            redisClient = redisClient,
            consumerGroup = "test",
            autoclaimMinIdleMs = redisProps.fetcher.autoclaimMinIdleMs,
            loops = loops,
            listeners = listeners.filter { it.group() == TEST_GROUP },
        )
    }

}
