package io.github.ktor_batterypack.redis

import io.github.ktor_batterypack.core.di.InitCallback
import org.koin.core.annotation.Module
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(KtorBatterypackRedisStreamsModule::class.java)

@Module(includes = [KtorBatterypackRedisModule::class])
class KtorBatterypackRedisStreamsModule {

    @Singleton(binds = [InitCallback::class, AutoCloseable::class])
    fun redisStreamFetchers(
        connectionFacade: RedisFacade,
        @Provided redisProps: RedisProps,
        listeners: List<RedisStreamListener>,
        loops: List<RedisStreamsBackgroundLoop>,
    ): RedisStreamFetchers {
        return RedisStreamFetchers(
            allListeners = listeners,
            redisProps = redisProps,
            connectionFacade = connectionFacade,
            loops = loops
        )
    }

}
