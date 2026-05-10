package io.github.kperczynski.infra.client

import io.github.kperczynski.domain.plant.FlorinPlantClient
import io.github.kperczynski.domain.plant.Plant
import io.github.kperczynski.libs.ktor.multipart.MultipartUpload
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton

@Singleton
class HttpFlorinPlantClient(
    @Named("florin") private val httpClient: HttpClient
) : FlorinPlantClient {

    override suspend fun identify(images: List<MultipartUpload>, tempIdentityId: String): Plant {
        val formData = formData {
            images.forEachIndexed { index, upload ->
                val fieldName = "image${index + 1}"

                val headers = Headers.build {
                    append(
                        HttpHeaders.ContentType,
                        upload.contentType?.toString() ?: ContentType.Image.JPEG.toString()
                    )
                    append(
                        HttpHeaders.ContentDisposition,
                        "filename=\"${upload.filename}\""
                    )
                }

                append(fieldName, upload.bytes, headers)
            }
        }

        val response = httpClient.submitFormWithBinaryData("/api/plant-identification", formData) {
            header("X-Temp-Identity-Id", tempIdentityId)
        }

        return when (response.status) {
            HttpStatusCode.OK ->
                response.body<Plant>()

            HttpStatusCode.UnprocessableEntity ->
                throw PlantIdentificationException("Unable to identify plant from the provided image")

            else ->
                throw PlantIdentificationException("Unexpected error from plant identification service: ${response.status}")
        }
    }
}

class PlantIdentificationException(message: String) : RuntimeException(message)
