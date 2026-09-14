package io.github.ktor_batterypack.core.ktor.client

import io.ktor.client.*
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.java.Java
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
import tools.jackson.databind.json.JsonMapper

/**
 * Factory for Ktor [HttpClient] instances with a common, pre-configured setup: Jackson 3 JSON
 * content negotiation, header-level logging, connect/request timeouts, and Micrometer metrics.
 *
 * Each client reports a Micrometer [io.micrometer.core.instrument.Timer] named
 * `ktor.http.client.requests` with the tags `method`, `status`, `host`, and `uri` (the declared
 * path pattern, or `UNKNOWN` when none is set), publishing the 0.5, 0.9, 0.95, and 0.99 percentiles.
 * Failures are recorded with an `error` tag and a synthetic status (`TIMEOUT`, `CONNECT_TIMEOUT`,
 * `IO_ERROR`, or `ERROR`).
 *
 * @param meterRegistry registry where client metrics are reported
 * @param jsonMapper shared Jackson mapper used for request/response bodies
 * @param engineFactory client engine to use, defaults to [Java]
 */
class KtorHttpClientFactory(
    private val meterRegistry: MeterRegistry,
    private val jsonMapper: JsonMapper,
    private val engineFactory: HttpClientEngineFactory<*> = Java
) {

    /**
     * Creates an [HttpClient] bound to [baseUrl] with the given timeouts, `expectSuccess` disabled,
     * and the standard plugins installed.
     */
    fun createHttpClient(baseUrl: String, connectTimeoutMs: Long, readTimeoutMs: Long): HttpClient =
        HttpClient(engineFactory) {
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
