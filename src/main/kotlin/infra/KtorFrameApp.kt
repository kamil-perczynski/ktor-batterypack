package io.github.kperczynski.infra

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.kperczynski.infra.client.FlorinClientProps
import io.github.kperczynski.libs.ktor.KtorHttpClientFactory
import io.github.kperczynski.infra.health.DatabaseReadinessCheck
import io.github.kperczynski.infra.health.DiskSpaceReadinessCheck
import io.github.kperczynski.libs.db.DatabaseProps
import io.github.kperczynski.libs.di.BannerPrinter
import io.github.kperczynski.libs.di.InitCallback
import io.github.kperczynski.libs.di.KoinLifecycleListener
import io.github.kperczynski.libs.di.LifecycleListener
import io.github.kperczynski.libs.health.HealthController
import io.github.kperczynski.libs.health.ReadinessCheck
import io.github.kperczynski.libs.health.ReadinessEndpoint
import io.github.kperczynski.libs.ktor.KtorController
import io.github.kperczynski.libs.ktor.KtorExceptionHandler
import io.github.kperczynski.libs.ktor.multipart.MultipartParser
import io.github.kperczynski.libs.ktor.multipart.MultipartProps
import io.ktor.client.HttpClient
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.annotation.*
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val log = LoggerFactory.getLogger(KtorFrameModule::class.java)

@KoinApplication
object KtorFrameApp

@Module(createdAtStart = true)
@ComponentScan("io.github.kperczynski")
@Configuration
class KtorFrameModule {

    @Singleton(createdAtStart = true)
    fun appConfig(): AppProps {
        val profiles = System.getenv("APP_PROFILES")?.split(",")?.map { it.trim() } ?: listOf("local")
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

@Module
@Configuration
class FlorinModule {

    @Singleton
    fun florinClientProps(appProps: AppProps): FlorinClientProps {
        return appProps.florin
    }

    @Singleton
    @Named("florin")
    fun florinHttpClient(factory: KtorHttpClientFactory, props: FlorinClientProps): HttpClient {
        return factory.createHttpClient(
            baseUrl = props.baseUrl,
            connectTimeoutMs = props.connectTimeoutMs,
            readTimeoutMs = props.readTimeoutMs
        )
    }
}

@Module
@Configuration
class DatabaseModule {

    @Singleton(createdAtStart = true, binds = [DataSource::class])
    fun dataSource(props: DatabaseProps): HikariDataSource {
        log.info("Connected to database at ${props.url} with pool size ${props.poolSize}")

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = props.url
            username = props.user
            password = props.password
            driverClassName = props.driver
            maximumPoolSize = props.poolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        return HikariDataSource(hikariConfig)
    }

    @Singleton(createdAtStart = true)
    fun database(dataSource: DataSource): Database {
        return Database.connect(dataSource)
    }

    @Singleton
    fun hikariCloser(dataSource: HikariDataSource): AutoCloseable {
        log.info("Registering AutoCloseable for DataSource: $dataSource")

        return AutoCloseable {
            log.info("Closing DataSource: {}", dataSource)
            dataSource.close()
            log.info("DataSource closed successfully.")
        }
    }

}

@Module
@Configuration
class ActuatorModule {

    @Singleton(binds = [ReadinessCheck::class])
    fun diskSpaceCheck(): DiskSpaceReadinessCheck {
        return DiskSpaceReadinessCheck()
    }

    @Singleton(binds = [ReadinessCheck::class])
    fun databaseCheck(dataSource: DataSource): DatabaseReadinessCheck {
        return DatabaseReadinessCheck(dataSource)
    }

    @Singleton
    fun readinessEndpoint(checks: List<ReadinessCheck>): ReadinessEndpoint {
        return ReadinessEndpoint(checks)
    }

    @Singleton(binds = [KtorController::class])
    fun healthController(readinessEndpoint: ReadinessEndpoint): HealthController {
        return HealthController(readinessEndpoint)
    }

}