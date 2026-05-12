package io.github.kperczynski.infra

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.github.kperczynski.configureServer
import io.github.kperczynski.libs.PostgresTestContainer
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking

open class KtorBatteriesIT {

    companion object {
        internal var httpClient: HttpClient
        internal var container: PostgresTestContainer = PostgresTestContainer()
        internal var application: Application

        init {
            container.start()

            System.setProperty("config.override.database.url", container.jdbcUrl)
            System.setProperty("config.override.database.username", container.username)
            System.setProperty("config.override.database.password", container.password)

            val builder = ApplicationTestBuilder()
            builder.environment { config = MapApplicationConfig("app.profiles" to "test") }
            builder.application { configureServer() }

            httpClient = builder.createClient {
                install(ContentNegotiation) {
                    jackson {
                        registerModule(KotlinModule.Builder().build())
                        registerModule(JavaTimeModule())
                        enable(SerializationFeature.INDENT_OUTPUT)
                        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    }
                }
                install(Logging) {
                    logger = Logger.DEFAULT
                    level = LogLevel.ALL
                }
            }

            application = builder.application

            runBlocking { builder.startApplication() }
        }
    }

}