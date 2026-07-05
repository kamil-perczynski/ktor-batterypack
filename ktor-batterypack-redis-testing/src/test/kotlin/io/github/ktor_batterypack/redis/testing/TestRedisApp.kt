package io.github.ktor_batterypack.redis.testing

import io.github.ktor_batterypack.core.KtorBatterypackCoreModule
import io.github.ktor_batterypack.core.ktor.KtorProps
import io.github.ktor_batterypack.metrics.KtorBatterypackMetricsModule
import io.github.ktor_batterypack.redis.KtorBatterypackRedisModule
import io.github.ktor_batterypack.redis.KtorBatterypackRedisStreamsModule
import io.github.ktor_batterypack.redis.RedisProps
import org.koin.core.annotation.*

@KoinApplication
object TestRedisApp

@Module(
    includes = [
        KtorBatterypackCoreModule::class,
        KtorBatterypackMetricsModule::class,
        KtorBatterypackRedisModule::class,
        KtorBatterypackRedisStreamsModule::class,
        TestRedisModule::class
    ]
)
@Configuration
@ComponentScan("io.github.ktor_batterypack")
class TestRedisAppModule {

    @Singleton
    fun redisProps(): RedisProps {
        return RedisProps(
            url = System.getProperty("config.override.redis.url", "redis://localhost:6379")
        )
    }

    @Singleton
    fun ktorProps(): KtorProps {
        return KtorProps()
    }

}
