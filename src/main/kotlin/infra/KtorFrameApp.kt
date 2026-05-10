package io.github.kperczynski.infra

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
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
import org.koin.core.annotation.*
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(KtorFrameModule::class.java)

@KoinApplication(
    modules = [
        KtorFrameModule::class,
        FlorinModule::class,
        DatabaseModule::class,
        MetricsModule::class
    ]
)
object KtorFrameApp

@Module
@ComponentScan("io.github.kperczynski")
@Configuration
class KtorFrameModule {

    @Singleton(createdAtStart = true)
    fun appConfig(): AppProps {
        val profiles =
            System.getenv("APP_PROFILES")?.split(",")?.map { it.trim() } ?: listOf("local")
        log.info("Loading application configuration with profiles: $profiles")
        return loadConfig(profiles)
    }

    @Singleton
    fun objectMapper(): ObjectMapper {
        return ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    @Singleton
    fun databaseProps(appProps: AppProps): DatabaseProps {
        return appProps.database
    }

    @Singleton
    fun multipartProps(appProps: AppProps): MultipartProps {
        return appProps.multipart
    }

    @Singleton
    fun multipartParser(props: MultipartProps): MultipartParser {
        return MultipartParser(props)
    }

    @Singleton
    fun ktorExceptionHandler(): KtorExceptionHandler {
        return KtorExceptionHandler()
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
