package io.github.kperczynski.domain.plant

import io.github.kperczynski.libs.ktor.multipart.MultipartUpload
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(PlantIdentificationService::class.java)

@Singleton
class PlantIdentificationService(
    private val florinPlantClient: FlorinPlantClient
) {

    suspend fun identify(uploads: List<MultipartUpload>, tempIdentityId: String): Plant {
        log.info("Processing plant identification with ${uploads.size} images")
        return florinPlantClient.identify(uploads, tempIdentityId)
    }
}
