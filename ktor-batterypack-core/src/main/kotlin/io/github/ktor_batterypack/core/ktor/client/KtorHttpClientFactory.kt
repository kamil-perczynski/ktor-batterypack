package io.github.ktor_batterypack.core.ktor.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.serialization.jackson3.JacksonConverter
import io.micrometer.core.instrument.MeterRegistry
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Singleton
import tools.jackson.databind.json.JsonMapper

@Singleton
class KtorHttpClientFactory(
    @Provided private val meterRegistry: MeterRegistry,
    private val jsonMapper: JsonMapper,
) {

    fun createHttpClient(
        baseUrl: String,
        connectTimeoutMs: Long,
        readTimeoutMs: Long
    ): HttpClient = HttpClient(CIO) {
        expectSuccess = false

        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(jsonMapper, true))
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }

        install(HttpTimeout) {
            connectTimeoutMillis = connectTimeoutMs
            requestTimeoutMillis = readTimeoutMs
        }

        install(ClientMicrometerMetricsPlugin) {
            registry = meterRegistry
        }

        defaultRequest {
            url(baseUrl)
        }
    }
}