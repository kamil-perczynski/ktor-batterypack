package io.github.ktor_batterypack.core.ktor

import io.github.ktor_batterypack.core.di.BootstrapCallback
import io.ktor.server.application.Application
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Singleton

/**
 * Registers all discovered [KtorPlugin] beans against the [Application] during bootstrap.
 */
@Singleton
class KtorPluginsBoostrapCallback(
    @Provided private val ktorApp: Application,
    private val plugins: List<KtorPlugin>
) : BootstrapCallback {

    override fun onBootstrap() {
        for (plugin in plugins) {
            plugin.register(ktorApp)
        }
    }

}
