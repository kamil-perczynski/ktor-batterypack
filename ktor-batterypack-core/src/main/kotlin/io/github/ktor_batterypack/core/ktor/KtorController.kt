package io.github.ktor_batterypack.core.ktor

import io.ktor.server.routing.Routing

/**
 * Contract for registering Ktor routes.
 */
interface KtorController {

    /**
     * Registers routes on the provided [routing].
     */
    fun register(routing: Routing)

}
