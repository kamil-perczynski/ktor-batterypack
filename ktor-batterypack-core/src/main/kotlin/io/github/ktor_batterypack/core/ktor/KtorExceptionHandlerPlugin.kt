package io.github.ktor_batterypack.core.ktor

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import org.koin.core.annotation.Singleton

@Singleton
class KtorExceptionHandlerPlugin(private val handlers: List<KtorExceptionHandler>) : KtorPlugin {

    override fun register(app: Application) {
        app.install(StatusPages) {
            handlers.forEach { handler -> handler.register(this) }
        }
    }

}