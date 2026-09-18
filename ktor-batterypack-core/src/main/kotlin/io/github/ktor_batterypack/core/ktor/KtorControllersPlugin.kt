package io.github.ktor_batterypack.core.ktor

import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(KtorControllersPlugin::class.java)

/**
 * Registers routes for all discovered [KtorController] beans on the [Application].
 */
@Singleton
class KtorControllersPlugin(
    @Provided private val ktorApp: Application,
    private val controllers: List<KtorController>
) : KtorPlugin {

    override fun register(app: Application) {
        ktorApp.routing {
            for (controller in controllers) {
                log.info("Registering routes for controller: {}", controller::class.simpleName)
                controller.register(this)
            }
        }
    }

}