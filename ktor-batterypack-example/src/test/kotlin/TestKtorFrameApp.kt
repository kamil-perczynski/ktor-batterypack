package io.github.kperczynski

import io.github.ktor_batterypack.core.KtorBatterypackCoreModule
import io.github.ktor_batterypack.database.KtorBatterypackDatabaseModule
import io.github.ktor_batterypack.metrics.KtorBatterypackMetricsModule
import io.github.ktor_batterypack.redis.KtorBatterypackRedisModule
import io.github.ktor_batterypack.redis.KtorBatterypackRedisStreamsModule
import io.github.ktor_batterypack.redis.testing.TestRedisModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module

@KoinApplication
object TestKtorFrameApp

@Module(
    includes = [
        KtorBatterypackCoreModule::class,
        KtorBatterypackDatabaseModule::class,
        KtorBatterypackMetricsModule::class,
        KtorBatterypackRedisModule::class,
        KtorBatterypackRedisStreamsModule::class,
        TestRedisModule::class
    ]
)
@Configuration
@ComponentScan("io.github.kperczynski")
class TestKtorFrameAppModule