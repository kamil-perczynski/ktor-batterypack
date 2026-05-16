package io.github.kperczynski.libs.ktor.client

import io.ktor.client.call.HttpClientCall
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.api.*
import io.ktor.client.request.*
import io.ktor.util.*
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.io.IOException
import java.util.concurrent.TimeUnit

val StartTimeKey = AttributeKey<Long>("MicrometerStartTime")
val PathPatternKey = AttributeKey<String>("MicrometerPathPattern")

fun HttpRequestBuilder.pathPattern(pattern: String) {
    attributes.put(PathPatternKey, pattern)
}

val ClientMicrometerMetricsPlugin = createClientPlugin("MicrometerMetricsPlugin", ::MetricsConfig) {
    val registry = pluginConfig.registry ?: error("MeterRegistry must be provided")
    val metricName = pluginConfig.metricName

    onRequest { request, _ ->
        request.attributes.put(StartTimeKey, System.nanoTime())
    }

    onResponse { response ->
        recordMetrics(response.call, response.status.value.toString(), registry, metricName)
    }

    on(Send) { request ->
        try {
            proceed(request)
        } catch (cause: Throwable) {
            val status = when (cause) {
                is HttpRequestTimeoutException -> "TIMEOUT"
                is ConnectTimeoutException -> "CONNECT_TIMEOUT"
                is SocketTimeoutException -> "TIMEOUT"
                is IOException -> "IO_ERROR"
                else -> "ERROR"
            }
            val startTime = request.attributes.getOrNull(StartTimeKey)
            if (startTime != null) {
                val durationNanos = System.nanoTime() - startTime
                val method = request.method.value
                val host = request.url.host
                val pathPattern = request.attributes.getOrNull(PathPatternKey) ?: "UNKNOWN"
                val errorName = cause.javaClass.simpleName

                Timer.builder(metricName)
                    .tag("method", method)
                    .tag("status", status)
                    .tag("host", host)
                    .tag("uri", pathPattern)
                    .tag("error", errorName)
                    .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                    .register(registry)
                    .record(durationNanos, TimeUnit.NANOSECONDS)
            }
            throw cause
        }
    }
}

private fun recordMetrics(
    call: HttpClientCall,
    status: String,
    registry: MeterRegistry,
    metricName: String
) {
    val startTime = call.attributes.getOrNull(StartTimeKey) ?: return
    val durationNanos = System.nanoTime() - startTime
    val method = call.request.method.value
    val host = call.request.url.host
    val pathPattern = call.attributes.getOrNull(PathPatternKey) ?: "UNKNOWN"

    Timer.builder(metricName)
        .tag("method", method)
        .tag("status", status)
        .tag("host", host)
        .tag("uri", pathPattern)
        .publishPercentiles(0.5, 0.9, 0.95, 0.99)
        .register(registry)
        .record(durationNanos, TimeUnit.NANOSECONDS)
}

class MetricsConfig {
    var registry: MeterRegistry? = null
    var metricName: String = "ktor.http.client.requests"
}
