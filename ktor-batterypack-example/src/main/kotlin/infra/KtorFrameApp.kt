package io.github.kperczynski.infra

import io.github.kperczynski.infra.florin.FlorinModule
import io.github.ktor_batterypack.core.KtorBatterypackCoreModule
import io.github.ktor_batterypack.core.ktor.KtorProps
import io.github.ktor_batterypack.database.DatabaseProps
import io.github.ktor_batterypack.database.KtorBatterypackDatabaseModule
import io.github.ktor_batterypack.metrics.KtorBatterypackMetricsModule
import io.github.ktor_batterypack.redis.KtorBatterypackRedisModule
import io.github.ktor_batterypack.redis.KtorBatterypackRedisStreamsModule
import io.github.ktor_batterypack.redis.RedisProps
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton
import javax.sql.DataSource

@KoinApplication(
    modules = [
        KtorBatterypackCoreModule::class,
        KtorFrameModule::class,
        FlorinModule::class,
        KtorBatterypackDatabaseModule::class,
        KtorBatterypackMetricsModule::class,
        KtorBatterypackRedisModule::class,
        KtorBatterypackRedisStreamsModule::class
    ]
)
object KtorFrameApp

@Module(
    includes = [
        KtorBatterypackDatabaseModule::class,
        KtorBatterypackMetricsModule::class,
    ]
)
@ComponentScan("io.github.kperczynski")
@Configuration
@Suppress("unused")
class KtorFrameModule {

    @Singleton
    fun databaseProps(configMap: ConfigMap): DatabaseProps {
        return configMap.database
    }

    @Singleton
    fun redisProps(configMap: ConfigMap): RedisProps {
        return configMap.redis
    }

    @Singleton
    fun ktorProps(configMap: ConfigMap): KtorProps {
        return configMap.ktor
    }

    @Singleton
    fun database(dataSource: DataSource): Database {
        return Database.connect(dataSource)
    }

}
