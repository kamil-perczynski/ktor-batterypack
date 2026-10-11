package io.github.ktor_batterypack.metrics.http.client

import io.github.ktor_batterypack.core.ktor.client.KtorHttpClientPlugin
import io.ktor.client.HttpClientConfig
import io.micrometer.core.instrument.MeterRegistry

/**
 * [KtorHttpClientPlugin] implementation that installs the Micrometer request-metrics plugin
 * ([ClientMicrometerMetricsPlugin]) into every client created by the shared [io.github.ktor_batterypack.core.ktor.client.KtorHttpClientFactory],
 * using the module's [MeterRegistry] as target.
 */
class HttpClientMetricsPlugin(private val meterRegistry: MeterRegistry) : KtorHttpClientPlugin {

    override fun register(config: HttpClientConfig<*>) {
        config.install(ClientMicrometerMetricsPlugin) {
            registry = meterRegistry
        }
    }

}
