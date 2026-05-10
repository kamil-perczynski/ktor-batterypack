package io.github.kperczynski.controllers

import io.github.kperczynski.domain.plant.PlantIdentificationService
import io.github.kperczynski.libs.ktor.KtorController
import io.github.kperczynski.libs.ktor.multipart.MultipartParser
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(PlantIdentificationController::class.java)

private const val MAX_IMAGES = 3

@Singleton
class PlantIdentificationController(
    private val plantIdentificationService: PlantIdentificationService,
    private val multipartParser: MultipartParser
) : KtorController {

    override fun register(routing: Routing) {
        routing.post("/api/plant-identification") {
            val tempIdentityId = call.request.queryParameters["tempIdentityId"]
                ?: throw IllegalArgumentException("Missing required query parameter: tempIdentityId")

            val multipart = call.receiveMultipart()
            val uploads = multipartParser.parseUploads(multipart, MAX_IMAGES)

            if (uploads.isEmpty()) {
                throw IllegalArgumentException("No images provided for plant identification")
            }

            val plant = plantIdentificationService.identify(uploads, tempIdentityId)

            call.respondNullable(HttpStatusCode.OK, plant)
        }
    }
}
