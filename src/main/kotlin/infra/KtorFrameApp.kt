package io.github.kperczynski.infra

import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import io.github.kperczynski.infra.florin.FlorinModule
import io.github.kperczynski.infra.monitoring.MetricsModule
import io.github.kperczynski.infra.persistence.DatabaseModule
import io.github.kperczynski.libs.db.DatabaseProps
import io.github.kperczynski.libs.di.BannerPrinter
import io.github.kperczynski.libs.di.InitCallback
import io.github.kperczynski.libs.di.KoinLifecycleListener
import io.github.kperczynski.libs.di.LifecycleListener
import io.github.kperczynski.libs.ktor.KtorExceptionHandler
import io.github.kperczynski.libs.ktor.multipart.MultipartParser
import io.github.kperczynski.libs.ktor.multipart.MultipartProps
import io.github.kperczynski.libs.redis.RedisModule
import io.github.kperczynski.libs.redis.RedisProps
import org.koin.core.annotation.*
import org.slf4j.LoggerFactory
import tools.jackson.databind.cfg.DateTimeFeature

private val log = LoggerFactory.getLogger(KtorFrameModule::class.java)

@KoinApplication(
    modules = [
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
    fun jsonMapper(): JsonMapper {
        return JsonMapper.builder()
            .addModule(KotlinModule.Builder().build())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build()
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
    fun multipartProps(appProps: AppProps): MultipartProps {
        return appProps.multipart
    }

    @Singleton
    fun multipartParser(props: MultipartProps): MultipartParser {
        return MultipartParser(props)
    }

    @Singleton(binds = [InitCallback::class])
    fun bannerPrinter(appProps: AppProps): BannerPrinter {
        return BannerPrinter(appProps.banner)
    }

    @Singleton(binds = [LifecycleListener::class])
    fun koinLifecycleListener(
        closeCallbacks: List<AutoCloseable>,
        initCallbacks: List<InitCallback>
    ): KoinLifecycleListener {
        return KoinLifecycleListener(closeCallbacks, initCallbacks)
    }

}
