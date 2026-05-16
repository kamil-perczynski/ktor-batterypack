package io.github.kperczynski.domain.plant

import io.github.kperczynski.domain.plant.model.Plant
import io.github.kperczynski.domain.plant.model.PlantDto
import io.github.kperczynski.domain.plant.model.PlantListing
import io.github.kperczynski.libs.ktor.multipart.MultipartUpload
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(PlantService::class.java)

@Singleton
class PlantService(
    private val plantIdentificationClient: PlantIdentificationClient,
    private val plantRepo: PlantRepo,
    private val plantEventPublisher: PlantEventPublisher
) {

    suspend fun identify(uploads: List<MultipartUpload>, tempIdentityId: String): Plant {
        log.info("Processing plant identification with ${uploads.size} images")
        val identifiedPlant = plantIdentificationClient.identify(uploads, tempIdentityId)

        val plant = toPlant(identifiedPlant)
        val savedPlant = plantRepo.create(plant)
        plantEventPublisher.publish(
            PlantEvent(
                plantId = savedPlant.id.toString(),
                type = PlantEventType.PLANT_CREATED,
                meta = mapOf("externalId" to savedPlant.externalId.toString())
            )
        )
        return savedPlant
    }

    suspend fun list(): PlantListing {
        log.info("Listing all plants")
        val plants = plantRepo.findAll()
        return PlantListing(items = plants)
    }

    suspend fun read(id: UInt): Plant {
        val plant = plantRepo.find(id)
        plantEventPublisher.publish(
            PlantEvent(
                plantId = plant.id.toString(),
                type = PlantEventType.PLANT_READ,
                meta = mapOf("externalId" to plant.externalId.toString())
            )
        )
        return plant
    }

}

private fun toPlant(identifiedPlant: PlantDto): Plant = Plant(
    id = 0u,
    externalId = identifiedPlant.id,
    createdAt = identifiedPlant.createdAt,
    status = identifiedPlant.status,
    identifications = identifiedPlant.identifications,
    displayName = identifiedPlant.displayName,
    species = identifiedPlant.species,
    cultivar = identifiedPlant.cultivar,
    healthStatus = identifiedPlant.healthStatus,
    careInstructions = identifiedPlant.careInstructions,
    difficulty = identifiedPlant.difficulty,
    toxicity = identifiedPlant.toxicity,
    images = identifiedPlant.images,
    tempIdentityId = identifiedPlant.tempIdentityId,
    lastWatered = identifiedPlant.lastWatered,
    soilMoisture = identifiedPlant.soilMoisture,
    repottedSinceBuying = identifiedPlant.repottedSinceBuying,
    lightLocation = identifiedPlant.lightLocation,
    nearHeatOrAc = identifiedPlant.nearHeatOrAc,
    lastWateringDate = identifiedPlant.lastWateringDate,
    lastFertilizationDate = identifiedPlant.lastFertilizationDate,
    lastRepottingDate = identifiedPlant.lastRepottingDate,
    lastStatusUpdateDate = identifiedPlant.lastStatusUpdateDate,
)
