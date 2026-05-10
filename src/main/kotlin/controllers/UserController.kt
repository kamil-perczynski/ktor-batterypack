package io.github.kperczynski.controllers

import io.github.kperczynski.domain.user.UserCreate
import io.github.kperczynski.domain.user.UserService
import io.github.kperczynski.domain.user.UserUpdate
import io.github.kperczynski.libs.ktor.KtorController
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.annotation.Singleton

@Singleton
class UserController(private val userService: UserService) : KtorController {

    override fun register(routing: Routing) {
        routing.post("/users") {
            val userCreate = call.receive<UserCreate>()
            val createdUser = userService.create(userCreate)
            call.respond(HttpStatusCode.Created, createdUser)
        }

        routing.get("/users/{id}") {
            val id = call.parameters["id"]?.toUInt() ?: throw IllegalArgumentException("Invalid ID")
            val user = userService.read(id)
            call.respond(HttpStatusCode.OK, user)
        }

        routing.put("/users/{id}") {
            val id = call.parameters["id"]?.toUInt() ?: throw IllegalArgumentException("Invalid ID")
            val userUpdate = call.receive<UserUpdate>()
            userService.update(id, userUpdate)
            call.respond(HttpStatusCode.NoContent)
        }

        routing.delete("/users/{id}") {
            val id = call.parameters["id"]?.toUInt() ?: throw IllegalArgumentException("Invalid ID")
            userService.delete(id)
            call.respond(HttpStatusCode.NoContent)
        }
    }

}
