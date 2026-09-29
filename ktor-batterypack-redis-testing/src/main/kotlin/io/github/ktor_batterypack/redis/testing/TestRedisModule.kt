package io.github.ktor_batterypack.redis.testing

import io.github.ktor_batterypack.redis.KtorBatterypackRedisModule
import io.github.ktor_batterypack.redis.monitoring.RedisStreamMetrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

const val TEST_CONSUMER_GROUP = "test"

@Module(includes = [KtorBatterypackRedisModule::class])
class TestRedisModule {

    @Singleton
    fun testRedisStreamMetrics(): RedisStreamMetrics {
        return RedisStreamMetrics(SimpleMeterRegistry())
    }

}
