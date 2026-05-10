package io.github.kperczynski.libs.ktor

import io.ktor.server.routing.Routing

interface KtorController {

    fun register(routing: Routing)

}