package io.github.kperczynski.controllers

import io.github.kperczynski.domain.plant.PlantService
import io.github.ktor_batterypack.core.ktor.KtorController
import io.github.kperczynski.libs.ktor.multipart.MultipartParser
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondNullable
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import org.koin.core.annotation.Singleton

private const val MAX_IMAGES = 3

@Singleton
class PlantController(
    private val plantService: PlantService,
    private val multipartParser: MultipartParser,
    private val plantDtoValidator: PlantDtoValidator
) : KtorController {

    override fun register(routing: Routing) {
        routing.get("/api/plants") {
            val listing = plantService.list()
            call.respond(HttpStatusCode.OK, listing)
        }

        routing.get("/api/plants/{id}") {
            val id = call.parameters["id"]?.toUInt() ?: throw IllegalArgumentException("Invalid ID")
            val plant = plantService.read(id)
            call.respond(HttpStatusCode.OK, plant)
        }

        routing.post("/api/plant-identification") {
            val tempIdentityId = call.request.queryParameters["tempIdentityId"]
                ?: throw IllegalArgumentException("Missing required query parameter: tempIdentityId")

            plantDtoValidator.validateIdentification(tempIdentityId)

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
