package io.github.ktor_batterypack.core.ktor

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import org.koin.core.annotation.Singleton
import tools.jackson.databind.json.JsonMapper

@Singleton
class ContentNegotiationKtorPlugin(private val jsonMapper: JsonMapper) : KtorPlugin {

    override fun register(app: Application) {
        app.install(ContentNegotiation) {
            jacksonSerialization(jsonMapper)
        }
    }

}