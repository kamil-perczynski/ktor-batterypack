package io.github.kperczynski.infra

import io.github.kperczynski.TestKtorFrameApp
import io.github.ktor_batterypack.core.config.loadConfig
import io.github.ktor_batterypack.core.configureKtorServer
import io.github.ktor_batterypack.database.testing.PostgresTestContainer
import io.github.ktor_batterypack.redis.testing.RedisTestContainer
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.jackson3.jackson
import io.ktor.server.application.Application
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.coroutines.runBlocking
import org.koin.dsl.module
import org.koin.plugin.module.dsl.withConfiguration
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.module.kotlin.KotlinModule

open class KtorBatteriesIT {

    companion object {
        private val container: PostgresTestContainer = PostgresTestContainer()
        private val redisContainer = RedisTestContainer()

        internal var application: Application
        internal var httpClient: HttpClient

        init {
            container.start()
            redisContainer.start()

            System.setProperty("config.override.database.url", container.jdbcUrl)
            System.setProperty("config.override.database.username", container.username)
            System.setProperty("config.override.database.password", container.password)
            System.setProperty("config.override.redis.url", redisContainer.redisUri)

            val builder = ApplicationTestBuilder()
            builder.environment { config = MapApplicationConfig("app.profiles" to "test") }
            builder.application {
                val ktorApp = this
                configureKtorServer { _, koinApp, profiles ->
                    koinApp.modules(
                        module {
                            single { ktorApp }
                            single { loadConfig<ConfigMap>(profiles) }
                        }
                    )
                    koinApp.withConfiguration<TestKtorFrameApp>()
                    koinApp.properties(mapOf("app.profiles" to profiles))
                }
            }

            httpClient = builder.createClient {
                install(ContentNegotiation) {
                    jackson {
                        addModule(KotlinModule.Builder().build())
                        enable(SerializationFeature.INDENT_OUTPUT)
                        disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    }
                }
                install(Logging) {
                    logger = Logger.DEFAULT
                    level = LogLevel.ALL
                }
            }

            application = builder.application

            runBlocking {
                builder.startApplication()
            }
        }
    }

}