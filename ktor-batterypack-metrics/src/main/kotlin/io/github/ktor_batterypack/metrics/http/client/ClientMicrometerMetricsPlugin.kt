package io.github.ktor_batterypack.metrics.http.client

import io.ktor.client.call.HttpClientCall
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.util.AttributeKey
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.io.IOException
import java.util.concurrent.TimeUnit

val StartTimeKey = AttributeKey<Long>("MicrometerStartTime")
val PathPatternKey = AttributeKey<String>("MicrometerPathPattern")

/**
 * Marks every request built from this [HttpRequestBuilder] with the [PathPatternKey] attribute so
 * the metrics plugin can report the declared path pattern (instead of interpolating actual paths)
 * in the `uri` tag.
 */
fun HttpRequestBuilder.pathPattern(pattern: String) {
    attributes.put(PathPatternKey, pattern)
}

/**
 * Ktor client plugin recording request durations as Micrometer [Timer]s.
 *
 * On success it reports the HTTP status as the `status` tag; on failure it reports a synthetic
 * status mapped from the thrown exception (`TIMEOUT`, `CONNECT_TIMEOUT`, `IO_ERROR`, or `ERROR`)
 * plus an `error` tag with the exception's simple class name.
 *
 * Must be configured with a [MetricsConfig.registry]; see [MetricsConfig.metricName] for the
 * default metric name.
 */
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

/**
 * Configuration for [ClientMicrometerMetricsPlugin].
 *
 * @property registry [MeterRegistry] the timers are registered in; required
 * @property metricName name of the recorded timers
 */
class MetricsConfig {
    var registry: MeterRegistry? = null
    var metricName: String = "ktor.http.client.requests"
}
