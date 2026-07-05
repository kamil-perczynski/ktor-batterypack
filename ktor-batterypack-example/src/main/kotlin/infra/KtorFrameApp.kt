package io.github.kperczynski.infra

import io.github.kperczynski.infra.florin.FlorinModule
import io.github.ktor_batterypack.core.KtorBatterypackCoreModule
import io.github.ktor_batterypack.core.config.loadConfig
import io.github.ktor_batterypack.core.ktor.KtorProps
import io.github.ktor_batterypack.database.DatabaseProps
import io.github.ktor_batterypack.database.KtorBatterypackDatabaseModule
import io.github.ktor_batterypack.database.MonitoredTransactions
import io.github.ktor_batterypack.metrics.KtorBatterypackMetricsModule
import io.github.ktor_batterypack.redis.KtorBatterypackRedisModule
import io.github.ktor_batterypack.redis.KtorBatterypackRedisStreamsModule
import io.github.ktor_batterypack.redis.RedisProps
import io.micrometer.core.instrument.MeterRegistry
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.annotation.*
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val log = LoggerFactory.getLogger(KtorFrameModule::class.java)

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
class KtorFrameModule {

    @Singleton
    fun appConfig(@Property("app.profiles") profiles: String): AppProps {
        val profileList = profiles.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        log.info("Loading application configuration with profiles: $profileList")
        return loadConfig(profileList)
    }

    @Singleton
    fun databaseProps(appProps: AppProps): DatabaseProps {
        return appProps.database
    }

    @Singleton
    fun redisProps(appProps: AppProps): RedisProps {
        return appProps.redis
    }

    @Singleton
    fun ktorProps(appProps: AppProps): KtorProps {
        return appProps.ktor
    }

    @Singleton
    fun database(dataSource: DataSource): Database {
        return Database.connect(dataSource)
    }

    @Singleton
    fun monitoredTransactions(database: Database, meterRegistry: MeterRegistry): MonitoredTransactions {
        return MonitoredTransactions(database, meterRegistry)
    }
}
