package io.github.kperczynski.infra.monitoring

import io.github.kperczynski.libs.ktor.KtorController
import io.github.kperczynski.libs.metrics.MetricsController
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Module
@Configuration
class MetricsModule {

    @Singleton(binds = [MeterRegistry::class])
    fun prometheusMeterRegistry(): PrometheusMeterRegistry {
        return PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    }

    @Singleton(binds = [KtorController::class])
    fun metricsController(registry: PrometheusMeterRegistry): MetricsController {
        return MetricsController(registry)
    }

}