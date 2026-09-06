package io.github.ktor_batterypack.core

import io.github.ktor_batterypack.core.di.LifecycleListener
import io.github.ktor_batterypack.core.ktor.KtorController
import io.github.ktor_batterypack.core.ktor.KtorExceptionHandler
import io.github.ktor_batterypack.core.ktor.jacksonSerialization
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.routing.*
import org.koin.core.KoinApplication
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.Koin
import org.koin.ktor.plugin.KoinApplicationStarted
import org.koin.ktor.plugin.KoinApplicationStopPreparing
import org.koin.ktor.plugin.koin
import org.koin.plugin.module.dsl.module
import tools.jackson.databind.json.JsonMapper

/**
 * Configures the Ktor application with Koin, exception handling, content negotiation, and controller routes.
 *
 * @param koinFn Callback to initialize the Koin application with resolved profiles.
 */
fun Application.configureKtorServer(koinFn: (ktorApp: Application, koinApp: KoinApplication, profiles: String) -> Unit) {
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
        koinFn(this@configureKtorServer, this, profiles)
    }

    val koin = koin()
    val ktorExceptionHandler: KtorExceptionHandler = koin.get()
    val jsonMapper: JsonMapper = koin.get()

    install(StatusPages) {
        ktorExceptionHandler.register(this)
    }

    install(ContentNegotiation) {
        jacksonSerialization(jsonMapper)
    }

    val controllers = koin.getAll<KtorController>()
    routing {
        for (controller in controllers) {
            log.info("Registering routes for controller: {}", controller::class.simpleName)
            controller.register(this)
        }
    }
}
