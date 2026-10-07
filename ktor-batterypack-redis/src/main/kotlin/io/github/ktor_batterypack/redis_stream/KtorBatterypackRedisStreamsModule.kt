package io.github.ktor_batterypack.redis_stream

import io.github.ktor_batterypack.core.di.InitCallback
import io.github.ktor_batterypack.redis.KtorBatterypackRedisModule
import io.github.ktor_batterypack.redis.RedisProps
import io.github.ktor_batterypack.redis_stream.monitoring.RedisStreamMetrics
import io.lettuce.core.RedisClient
import io.micrometer.core.instrument.MeterRegistry
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Singleton

@Module(includes = [KtorBatterypackRedisModule::class])
@ComponentScan("io.github.ktor_batterypack.redis_stream")
class KtorBatterypackRedisStreamsModule {

    @Singleton
    fun redisStreamMetrics(@Provided meterRegistry: MeterRegistry): RedisStreamMetrics {
        return RedisStreamMetrics(meterRegistry)
    }

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
