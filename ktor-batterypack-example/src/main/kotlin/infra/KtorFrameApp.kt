package io.github.kperczynski.infra

import com.sksamuel.hoplite.indent
import io.github.kperczynski.controllers.AppPropsValidator.Companion.appPropsValidator
import io.github.kperczynski.infra.florin.FlorinModule
import io.github.ktor_batterypack.core.KtorBatterypackCoreModule
import io.github.ktor_batterypack.core.config.loadConfig
import io.github.ktor_batterypack.core.ktor.KtorProps
import io.github.ktor_batterypack.database.DatabaseProps
import io.github.ktor_batterypack.database.KtorBatterypackDatabaseModule
import io.github.ktor_batterypack.metrics.KtorBatterypackMetricsModule
import io.github.ktor_batterypack.redis.KtorBatterypackRedisModule
import io.github.ktor_batterypack.redis.KtorBatterypackRedisStreamsModule
import io.github.ktor_batterypack.redis.RedisProps
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.annotation.*
import org.slf4j.LoggerFactory
import tools.jackson.databind.json.JsonMapper
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
@Suppress("unused")
class KtorFrameModule {

    @Singleton
    fun appConfig(@Property("app.profiles") profiles: String, jsonMapper: JsonMapper): AppProps {
        val profileList = profiles.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        log.info("Loading application configuration with profiles: $profileList")
        val config = loadConfig<AppProps>(profileList)

        return appPropsValidator.validate(config).fold({ it }, { _, errors ->
            val json = jsonMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(errors)
                .indent("  ")

            throw IllegalStateException("Application config is invalid. Check the following errors:\n$json")
        })
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

}
