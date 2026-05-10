package io.github.kperczynski.controllers

import io.ktor.server.routing.Routing

interface KtorController {

    fun register(routing: Routing)

}