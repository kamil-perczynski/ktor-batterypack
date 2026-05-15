package io.github.kperczynski.infra.client

import tools.jackson.databind.JsonNode
import io.github.kperczynski.domain.plant.model.PlantDto
import io.github.kperczynski.domain.plant.PlantIdentificationClient
import io.github.kperczynski.libs.ktor.multipart.MultipartUpload
import io.github.kperczynski.libs.ktor.pathPattern
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
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
