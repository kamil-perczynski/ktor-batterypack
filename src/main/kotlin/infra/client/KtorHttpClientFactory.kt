package io.github.kperczynski.infra.client

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.jackson.*
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
