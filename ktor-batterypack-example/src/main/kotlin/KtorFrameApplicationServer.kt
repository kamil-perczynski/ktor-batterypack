package io.github.kperczynski

import io.github.kperczynski.infra.ConfigMap
import io.github.kperczynski.infra.KtorFrameApp
import io.github.ktor_batterypack.core.config.loadConfig
import io.github.ktor_batterypack.core.configureKtorServer
import io.github.ktor_batterypack.metrics.reflect.enableTimedMethodsSampling
import io.ktor.server.application.*
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.routing
import io.ktor.server.webjars.Webjars
import org.koin.dsl.module
import org.koin.plugin.module.dsl.withConfiguration

fun Application.configureServer() {
    configureKtorServer { ktorApp, koinApp, profiles ->
        koinApp.modules(
            module {
                single { loadConfig<ConfigMap>(profiles) }
                single { ktorApp }
            }
        )
        koinApp.withConfiguration<KtorFrameApp>()
        enableTimedMethodsSampling(koinApp)

        install(Webjars)

        routing {
            staticResources("/", "static")
        }
    }
}



