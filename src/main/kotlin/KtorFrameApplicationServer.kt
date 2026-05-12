package io.github.kperczynski

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.kperczynski.infra.KtorFrameApp
import io.github.kperczynski.libs.di.LifecycleListener
import io.github.kperczynski.libs.ktor.KtorController
import io.github.kperczynski.libs.ktor.KtorExceptionHandler
import io.github.kperczynski.libs.ktor.jacksonSerialization
import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.koin.dsl.koinConfiguration
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.Koin
import org.koin.ktor.plugin.KoinApplicationStarted
import org.koin.ktor.plugin.KoinApplicationStopPreparing
import org.koin.ktor.plugin.koin
import org.koin.plugin.module.dsl.withConfiguration

fun Application.configureServer() {
    monitor.subscribe(KoinApplicationStarted) {
        log.debug("Application has started. Notifying lifecycle listener")
        val lifecycleListener: LifecycleListener = get()
        lifecycleListener.onStart()
    }

    monitor.subscribe(KoinApplicationStopPreparing) {
        log.debug("Application is stopping. Notifying lifecycle listener")
        val lifecycleListener: LifecycleListener = get()
        lifecycleListener.onStop()
    }

    val profiles = environment.config.propertyOrNull("app.profiles")?.getString()
        ?: System.getenv("APP_PROFILES")
        ?: "local"

    install(Koin) {
        withConfiguration<KtorFrameApp>()
        properties(mapOf("app.profiles" to profiles))
        createEagerInstances()
    }

    val koin = koin()
    val ktorExceptionHandler: KtorExceptionHandler = koin.get()
    val objectMapper: ObjectMapper = koin.get()

    install(StatusPages) {
        ktorExceptionHandler.register(this)
    }

    install(ContentNegotiation) {
        jacksonSerialization(objectMapper)
    }

    val meterRegistry: PrometheusMeterRegistry = koin.get()
    install(MicrometerMetrics) {
        registry = meterRegistry
    }

    val controllers = koin.getAll<KtorController>()
    routing {
        for (controller in controllers) {
            log.info("Registering routes for controller: {}", controller::class.simpleName)
            controller.register(this)
        }
    }
}
