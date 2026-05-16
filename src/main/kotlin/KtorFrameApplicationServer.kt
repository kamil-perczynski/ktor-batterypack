package io.github.kperczynski

import io.github.kperczynski.infra.KtorFrameApp
import io.github.kperczynski.libs.di.LifecycleListener
import io.github.kperczynski.libs.ktor.KtorController
import io.github.kperczynski.libs.ktor.KtorExceptionHandler
import io.github.kperczynski.libs.ktor.jacksonSerialization
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
import org.koin.plugin.module.dsl.withConfiguration
import tools.jackson.databind.json.JsonMapper

internal fun configureKtorServer(app: Application, koinFn: KoinApplication.(profiles: String) -> Unit) {
    app.monitor.subscribe(KoinApplicationStarted) {
        app.log.debug("Application has started. Notifying lifecycle listener")
        val lifecycleListener: LifecycleListener = app.get()
        lifecycleListener.onStart()
    }

    app.monitor.subscribe(KoinApplicationStopPreparing) {
        app.log.debug("Application is stopping. Notifying lifecycle listener")
        val lifecycleListener: LifecycleListener = app.get()
        lifecycleListener.onStop()
    }

    val profiles = app.environment.config.propertyOrNull("app.profiles")?.getString()
        ?: System.getenv("APP_PROFILES")
        ?: "local"

    app.install(Koin) {
        koinFn(profiles)
    }

    val koin = app.koin()
    val ktorExceptionHandler: KtorExceptionHandler = koin.get()
    val jsonMapper: JsonMapper = koin.get()

    app.install(StatusPages) {
        ktorExceptionHandler.register(this)
    }

    app.install(ContentNegotiation) {
        jacksonSerialization(jsonMapper)
    }

    val controllers = koin.getAll<KtorController>()
    app.routing {
        for (controller in controllers) {
            app.log.info("Registering routes for controller: {}", controller::class.simpleName)
            controller.register(this)
        }
    }
}

fun Application.configureServer() {
    val ktorApp = this

    configureKtorServer(ktorApp) { profiles ->
        modules(
            module {
                single { ktorApp }
            }
        )
        withConfiguration<KtorFrameApp>()
        properties(mapOf("app.profiles" to profiles))
    }
}
