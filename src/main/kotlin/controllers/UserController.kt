package io.github.kperczynski.controllers

import io.github.kperczynski.UserService
import io.github.kperczynski.repository.User
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.annotation.Singleton

@Singleton
class UserController(private val userService: UserService) : KtorController {

    override fun register(routing: Routing) {
        routing.post("/users") {
            val user = call.receive<User>()
            val id = userService.create(user)
            call.respond(HttpStatusCode.Created, id)
        }

        routing.get("/users/{id}") {
            val id = call.parameters["id"]?.toUInt() ?: throw IllegalArgumentException("Invalid ID")
            val user = userService.read(id)
            call.respond(HttpStatusCode.OK, user)
        }

        routing.put("/users/{id}") {
            val id = call.parameters["id"]?.toUInt() ?: throw IllegalArgumentException("Invalid ID")
            val user = call.receive<User>()
            userService.update(id, user)
            call.respond(HttpStatusCode.NoContent)
        }

        routing.delete("/users/{id}") {
            val id = call.parameters["id"]?.toUInt() ?: throw IllegalArgumentException("Invalid ID")
            userService.delete(id)
            call.respond(HttpStatusCode.NoContent)
        }
    }

}
