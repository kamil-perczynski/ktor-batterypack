package io.github.kperczynski

import io.github.kperczynski.infra.KtorFrameApp
import io.github.ktor_batterypack.core.configureKtorServer
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.plugin.module.dsl.withConfiguration

fun Application.configureServer() {
    configureKtorServer { ktorApp, koinApp, profiles ->
        koinApp.modules(
            module {
                single { ktorApp }
            }
        )
        koinApp.withConfiguration<KtorFrameApp>()
        koinApp.properties(mapOf("app.profiles" to profiles))
        enableTimedRepositories(koinApp)
    }
}



