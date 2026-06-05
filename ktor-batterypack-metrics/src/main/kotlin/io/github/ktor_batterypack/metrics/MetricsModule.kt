package io.github.ktor_batterypack.metrics

import io.github.ktor_batterypack.core.ktor.KtorController
import io.ktor.server.application.Application
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Singleton

@Module
@Configuration
class MetricsModule {

    @Singleton(binds = [MeterRegistry::class])
    fun prometheusMeterRegistry(bootstrapper: List<MeterRegistryInit>): PrometheusMeterRegistry {
        val reg = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        // ktor registers metric filters that need to be registered before any metrics
        // otherwise, a warning is shown
        bootstrapper.forEach { it.init(reg) }
        return reg
    }

    @Singleton(binds = [MeterRegistryInit::class])
    fun ktorMetricsInit(@Provided application: Application): KtorMetricsInit {
        return KtorMetricsInit(application)
    }

    @Singleton(binds = [KtorController::class])
    fun metricsController(registry: PrometheusMeterRegistry): MetricsController {
        return MetricsController(registry)
    }

}
