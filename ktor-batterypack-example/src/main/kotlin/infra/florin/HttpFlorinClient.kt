package io.github.kperczynski.infra.client

import tools.jackson.databind.JsonNode
import io.github.kperczynski.domain.plant.model.PlantDto
import io.github.kperczynski.domain.plant.PlantIdentificationClient
import io.github.ktor_batterypack.core.multipart.MultipartUpload
import io.github.ktor_batterypack.core.ktor.client.pathPattern
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
class HttpFlorinClient(
    @Named("florin") private val httpClient: HttpClient
) : PlantIdentificationClient {

    override suspend fun identify(
        images: List<MultipartUpload>,
        tempIdentityId: String
    ): PlantDto {
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
            pathPattern("/api/plant-identification")
        }

        return when (response.status) {
            HttpStatusCode.OK ->
                response.body<PlantDto>()

            HttpStatusCode.UnprocessableEntity -> {
                val problemDetail = response.body<JsonNode>()
                throw PlantIdentificationException("Unable to identify plant from the provided image: $problemDetail")
            }


            else ->
                throw PlantIdentificationException("Unexpected error from plant identification service: ${response.status}")
        }
    }
}

class PlantIdentificationException(message: String) : RuntimeException(message)
