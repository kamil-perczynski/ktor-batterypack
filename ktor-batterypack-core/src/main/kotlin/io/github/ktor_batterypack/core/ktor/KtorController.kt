package io.github.ktor_batterypack.core.ktor

import io.ktor.server.routing.Routing

interface KtorController {

    fun register(routing: Routing)

}
