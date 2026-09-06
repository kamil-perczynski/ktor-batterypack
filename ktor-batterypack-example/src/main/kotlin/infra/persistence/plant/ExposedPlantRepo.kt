package io.github.kperczynski.infra.persistence.plant

import io.github.kperczynski.domain.plant.PlantRepo
import io.github.kperczynski.domain.plant.model.Plant
import io.github.kperczynski.domain.plant.model.enums.LastWateredOption
import io.github.kperczynski.domain.plant.model.enums.LightLocationOption
import io.github.kperczynski.domain.plant.model.enums.PlantDifficulty
import io.github.kperczynski.domain.plant.model.enums.PlantStatus
import io.github.kperczynski.domain.plant.model.enums.SoilMoistureOption
import io.github.kperczynski.domain.plant.model.enums.YesNoOption
import io.github.ktor_batterypack.core.di.InitCallback
import io.github.ktor_batterypack.core.exception.ResourceMissingException
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory
import java.util.UUID

private val log = LoggerFactory.getLogger(ExposedPlantRepo::class.java)

@Singleton
class ExposedPlantRepo(private val database: Database) : PlantRepo, InitCallback {

    override fun onInit() {
        transaction(database) {
            log.info("Creating 'plants' table if it does not exist...")
            SchemaUtils.create(PlantTable)
        }
    }

    override suspend fun create(plant: Plant): Plant {
        return suspendTransaction(database) {
            val entity = PlantEntity.new {
                externalId = plant.externalId
                createdAt = plant.createdAt
                status = plant.status.name
                identifications = plant.identifications
                displayName = plant.displayName
                species = plant.species
                cultivar = plant.cultivar
                healthStatus = plant.healthStatus
                careInstructions = plant.careInstructions
                difficulty = plant.difficulty.name
                toxicity = plant.toxicity
                images = plant.images
                tempIdentityId = plant.tempIdentityId
                lastWatered = plant.lastWatered?.name
                soilMoisture = plant.soilMoisture?.name
                repottedSinceBuying = plant.repottedSinceBuying?.name
                lightLocation = plant.lightLocation?.name
                nearHeatOrAc = plant.nearHeatOrAc?.name
                lastWateringDate = plant.lastWateringDate
                lastFertilizationDate = plant.lastFertilizationDate
                lastRepottingDate = plant.lastRepottingDate
                lastStatusUpdateDate = plant.lastStatusUpdateDate
            }
            toPlant(entity)
        }
    }

    override suspend fun find(id: UInt): Plant {
        return suspendTransaction(database) {
            val entity = PlantEntity.findById(id)
                ?: throw ResourceMissingException(Plant::class.java, id)
            toPlant(entity)
        }
    }

    override suspend fun findAll(): List<Plant> {
        return suspendTransaction(database) {
            PlantEntity.all().map { toPlant(it) }
        }
    }

    override suspend fun update(plant: Plant) {
        suspendTransaction(database) {
            val entity = PlantEntity.findById(plant.id)
                ?: throw ResourceMissingException(Plant::class.java, plant.id)
            entity.status = plant.status.name
            entity.identifications = plant.identifications
            entity.displayName = plant.displayName
            entity.species = plant.species
            entity.cultivar = plant.cultivar
            entity.healthStatus = plant.healthStatus
            entity.careInstructions = plant.careInstructions
            entity.difficulty = plant.difficulty.name
            entity.toxicity = plant.toxicity
            entity.images = plant.images
            entity.tempIdentityId = plant.tempIdentityId
            entity.lastWatered = plant.lastWatered?.name
            entity.soilMoisture = plant.soilMoisture?.name
            entity.repottedSinceBuying = plant.repottedSinceBuying?.name
            entity.lightLocation = plant.lightLocation?.name
            entity.nearHeatOrAc = plant.nearHeatOrAc?.name
            entity.lastWateringDate = plant.lastWateringDate
            entity.lastFertilizationDate = plant.lastFertilizationDate
            entity.lastRepottingDate = plant.lastRepottingDate
            entity.lastStatusUpdateDate = plant.lastStatusUpdateDate
        }
    }

    override suspend fun delete(id: UInt) {
        suspendTransaction(database) {
            PlantEntity.findById(id)?.delete()
        }
    }

    override suspend fun findByTempIdentityId(tempIdentityId: UUID): Plant? {
        return suspendTransaction(database) {
            PlantEntity.find { PlantTable.tempIdentityId eq tempIdentityId }
                .singleOrNull()
                ?.let { toPlant(it) }
        }
    }

    private fun toPlant(entity: PlantEntity): Plant {
        return Plant(
            id = entity.id.value,
            externalId = entity.externalId,
            createdAt = entity.createdAt,
            status = PlantStatus.valueOf(entity.status),
            identifications = entity.identifications,
            displayName = entity.displayName,
            species = entity.species,
            cultivar = entity.cultivar,
            healthStatus = entity.healthStatus,
            careInstructions = entity.careInstructions,
            difficulty = PlantDifficulty.valueOf(entity.difficulty),
            toxicity = entity.toxicity,
            images = entity.images,
            tempIdentityId = entity.tempIdentityId,
            lastWatered = entity.lastWatered?.let { LastWateredOption.valueOf(it) },
            soilMoisture = entity.soilMoisture?.let { SoilMoistureOption.valueOf(it) },
            repottedSinceBuying = entity.repottedSinceBuying?.let { YesNoOption.valueOf(it) },
            lightLocation = entity.lightLocation?.let { LightLocationOption.valueOf(it) },
            nearHeatOrAc = entity.nearHeatOrAc?.let { YesNoOption.valueOf(it) },
            lastWateringDate = entity.lastWateringDate,
            lastFertilizationDate = entity.lastFertilizationDate,
            lastRepottingDate = entity.lastRepottingDate,
            lastStatusUpdateDate = entity.lastStatusUpdateDate
        )
    }
}
