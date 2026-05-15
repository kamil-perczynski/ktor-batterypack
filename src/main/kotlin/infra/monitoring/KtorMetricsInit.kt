package io.github.kperczynski.infra.monitoring

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.micrometer.core.instrument.MeterRegistry

class KtorMetricsInit(private val ktorApp: Application) : MeterRegistryInit {
    override fun init(meterRegistry: MeterRegistry) {
        ktorApp.install(MicrometerMetrics) {
            registry = meterRegistry
        }
    }
}