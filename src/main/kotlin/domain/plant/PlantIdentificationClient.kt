package io.github.kperczynski.domain.plant

import io.github.kperczynski.domain.plant.model.PlantDto
import io.github.kperczynski.libs.ktor.multipart.MultipartUpload

interface PlantIdentificationClient {
    suspend fun identify(images: List<MultipartUpload>, tempIdentityId: String): PlantDto
}
