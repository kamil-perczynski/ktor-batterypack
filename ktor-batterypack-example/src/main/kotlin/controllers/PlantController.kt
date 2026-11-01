package io.github.kperczynski.controllers

import io.github.kperczynski.domain.plant.PlantService
import io.github.kperczynski.infra.oidc.OidcAuthScheme
import io.github.ktor_batterypack.authz.PermissionChecker
import io.github.ktor_batterypack.core.ktor.KtorController
import io.github.ktor_batterypack.core.multipart.MultipartParser
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticateWith
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondNullable
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.utils.io.ExperimentalKtorApi
import org.koin.core.annotation.Singleton

private const val MAX_IMAGES = 3

@Singleton
class PlantController(
    private val plantService: PlantService,
    private val multipartParser: MultipartParser,
    private val authScheme: OidcAuthScheme,
    private val permissionChecker: PermissionChecker,
) : KtorController {

    @OptIn(ExperimentalKtorApi::class)
    override fun register(routing: Routing) {
        routing.authenticateWith(authScheme.jwt) {
            get("/api/plants") {
                permissionChecker.checkPermission(call.principal, PLANT_LIST)
                val listing = plantService.list()
                call.respond(HttpStatusCode.OK, listing)
            }

            get("/api/plants/{id}") {
                permissionChecker.checkPermission(call.principal, PLANT_READ)
                val id = call.parameters["id"]?.toUInt() ?: throw IllegalArgumentException("Invalid ID")
                val plant = plantService.read(id)
                call.respond(HttpStatusCode.OK, plant)
            }

            post("/api/plant-identification") {
                permissionChecker.checkPermission(call.principal, PLANT_IDENTIFY)
                val tempIdentityId = call.request.queryParameters["tempIdentityId"]
                    ?: throw IllegalArgumentException("Missing required query parameter: tempIdentityId")

                val multipart = call.receiveMultipart()
                val uploads = multipartParser.parseUploads(multipart, MAX_IMAGES)

                if (uploads.isEmpty()) {
                    throw IllegalArgumentException("No images provided for plant identification")
                }

                val plant = plantService.identify(uploads, tempIdentityId)

                call.respondNullable(HttpStatusCode.OK, plant)
            }
        }
    }
}
