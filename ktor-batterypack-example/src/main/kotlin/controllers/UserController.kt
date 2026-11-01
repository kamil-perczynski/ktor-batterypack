package io.github.kperczynski.controllers

import io.github.kperczynski.controllers.UserApiValidator
import io.github.kperczynski.domain.user.UserCreate
import io.github.kperczynski.domain.user.UserService
import io.github.kperczynski.domain.user.UserUpdate
import io.github.kperczynski.infra.oidc.OidcAuthScheme
import io.github.ktor_batterypack.authz.PermissionChecker
import io.github.ktor_batterypack.core.ktor.JsonBinder
import io.github.ktor_batterypack.core.ktor.KtorController
import io.ktor.http.*
import io.ktor.server.auth.authenticateWith
import io.ktor.server.auth.principal
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.ExperimentalKtorApi
import org.koin.core.annotation.Singleton

@Singleton
class UserController(
    private val userService: UserService,
    private val jsonBinder: JsonBinder,
    private val authScheme: OidcAuthScheme,
    private val permissionChecker: PermissionChecker,
) : KtorController {

    @OptIn(ExperimentalKtorApi::class)
    override fun register(routing: Routing) {
        routing.authenticateWith(authScheme.jwt) {
            post("/users") {
                permissionChecker.checkPermission(call.principal, USER_CREATE)
                val userCreate = jsonBinder.bindBody<UserCreate>(
                    call = call,
                    validatorFn = UserApiValidator::validateUserCreate
                )

                val createdUser = userService.create(userCreate)
                call.respond(HttpStatusCode.Created, createdUser)
            }

            get("/users/{id}") {
                permissionChecker.checkPermission(call.principal, USER_READ)
                val id = call.parameters["id"]?.toUInt() ?: throw IllegalArgumentException("Invalid ID")
                val user = userService.read(id)
                call.respond(HttpStatusCode.OK, user)
            }

            put("/users/{id}") {
                permissionChecker.checkPermission(call.principal, USER_UPDATE)
                val id = call.parameters["id"]?.toUInt() ?: throw IllegalArgumentException("Invalid ID")
                val userUpdate = jsonBinder.bindBody<UserUpdate>(call, UserApiValidator::validateUserCreate)
                userService.update(id, userUpdate)
                call.respond(HttpStatusCode.NoContent)
            }

            delete("/users/{id}") {
                permissionChecker.checkPermission(call.principal, USER_DELETE)
                val id = call.parameters["id"]?.toUInt() ?: throw IllegalArgumentException("Invalid ID")
                userService.delete(id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }

}
