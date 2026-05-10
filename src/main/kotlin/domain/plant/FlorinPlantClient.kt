package io.github.kperczynski.domain.plant

import io.github.kperczynski.libs.ktor.multipart.MultipartUpload

interface FlorinPlantClient {
    suspend fun identify(images: List<MultipartUpload>, tempIdentityId: String): Plant
}
