package io.github.kperczynski.infra

import io.github.kperczynski.infra.florin.FlorinModule
import io.github.ktor_batterypack.core.KtorBatterypackCoreModule
import io.github.ktor_batterypack.core.config.loadConfig
import io.github.ktor_batterypack.core.ktor.KtorProps
import io.github.ktor_batterypack.metrics.MetricsModule
import io.github.kperczynski.infra.persistence.DatabaseModule
import io.github.kperczynski.libs.db.DatabaseProps
import io.github.kperczynski.libs.redis.RedisModule
import io.github.kperczynski.libs.redis.RedisProps
import org.koin.core.annotation.*
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(KtorFrameModule::class.java)

@KoinApplication(
    modules = [
        KtorBatterypackCoreModule::class,
        KtorFrameModule::class,
        FlorinModule::class,
        DatabaseModule::class,
        MetricsModule::class,
        RedisModule::class
    ]
)
object KtorFrameApp

@Module
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

}
