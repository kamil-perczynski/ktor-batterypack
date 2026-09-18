package io.github.ktor_batterypack.core

import io.github.ktor_batterypack.core.di.LifecycleListener
import io.ktor.server.application.*
import org.koin.core.KoinApplication
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.Koin
import org.koin.ktor.plugin.KoinApplicationStarted
import org.koin.ktor.plugin.KoinApplicationStopPreparing

/**
 * Configures the Ktor application with Koin, exception handling, content negotiation, and controller routes.
 *
 * @param koinFn Callback to initialize the Koin application with resolved profiles.
 */
fun Application.configureKtorServer(koinFn: (ktorApp: Application, koinApp: KoinApplication, profiles: List<String>) -> Unit) {
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

    val profiles = resolveProfiles(this)
    log.info("Loading application configuration with profiles: $profiles")

    install(Koin) {
        koinFn(this@configureKtorServer, this, profiles)
        koin.get<LifecycleListener>().onBootstrap()
    }
}

private fun resolveProfiles(app: Application): List<String> {
    val rawProfiles = app.environment.config.propertyOrNull("app.profiles")?.getString()
        ?: System.getenv("APP_PROFILES")
        ?: "local"
    val profiles = rawProfiles.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    return profiles
}
