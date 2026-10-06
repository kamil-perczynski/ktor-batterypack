package io.github.ktor_batterypack.redis

import io.github.ktor_batterypack.core.di.InitCallback
import io.lettuce.core.RedisClient
import org.koin.core.annotation.Module
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Singleton

@Module(includes = [KtorBatterypackRedisModule::class])
class KtorBatterypackRedisStreamsModule {

    @Singleton(binds = [InitCallback::class, AutoCloseable::class])
    fun redisStreamFetchers(
        redisClient: RedisClient,
        @Provided redisProps: RedisProps,
        listeners: List<RedisStreamListener>,
        loops: List<RedisStreamsBackgroundLoop>,
    ): RedisStreamFetchers {
        return RedisStreamFetchers(
            allListeners = listeners,
            redisProps = redisProps,
            redisClient = redisClient,
            loops = loops
        )
    }

}
