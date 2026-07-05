package io.github.ktor_batterypack.redis

import io.github.ktor_batterypack.core.di.InitCallback
import io.github.ktor_batterypack.redis.RedisStreamListenerGroups.Companion.MAIN_GROUP
import io.lettuce.core.RedisClient
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Singleton

@Module(includes = [KtorBatterypackRedisModule::class])
class KtorBatterypackRedisStreamsModule {

    @Singleton(binds = [InitCallback::class, AutoCloseable::class])
    @Named("redisFetcher")
    fun redisFetcher(
        redisClient: RedisClient,
        @Provided redisProps: RedisProps,
        listeners: List<RedisStreamListener>,
        loops: List<RedisStreamsBackgroundLoop>,
    ): RedisStreamFetcher {
        return RedisStreamFetcher(
            consumerId = redisProps.fetcher.consumerPrefix + System.currentTimeMillis()
                .toHexString(),
            redisClient = redisClient,
            consumerGroup = redisProps.fetcher.consumerGroup,
            autoclaimMinIdleMs = redisProps.fetcher.autoclaimMinIdleMs,
            loops = loops,
            listeners = listeners.filter { it.group() == MAIN_GROUP },
        )
    }

}
