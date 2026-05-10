package io.github.kperczynski.libs.ktor

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.EMPTY
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.LoggingConfig
import io.ktor.serialization.jackson.jackson
import org.koin.core.annotation.Singleton

@Singleton
class KtorHttpClientFactory {

    fun createHttpClient(
        baseUrl: String,
        connectTimeoutMs: Long,
        readTimeoutMs: Long
    ): HttpClient = HttpClient(CIO) {
        expectSuccess = false

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
            logger = Logger.EMPTY
            level = LogLevel.HEADERS
        }

        install(HttpTimeout) {
            connectTimeoutMillis = connectTimeoutMs
            requestTimeoutMillis = readTimeoutMs
        }

        defaultRequest {
            url(baseUrl)
        }
    }
}