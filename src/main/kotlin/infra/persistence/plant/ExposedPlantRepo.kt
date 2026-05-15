package io.github.kperczynski.infra.persistence.plant

import tools.jackson.databind.json.JsonMapper
import io.github.kperczynski.domain.plant.model.Plant
import io.github.kperczynski.domain.plant.PlantRepo
import io.github.kperczynski.domain.plant.model.enums.LastWateredOption
import io.github.kperczynski.domain.plant.model.enums.LightLocationOption
import io.github.kperczynski.domain.plant.model.enums.PlantDifficulty
import io.github.kperczynski.domain.plant.model.enums.PlantStatus
import io.github.kperczynski.domain.plant.model.enums.SoilMoistureOption
import io.github.kperczynski.domain.plant.model.enums.YesNoOption
import io.github.kperczynski.libs.di.InitCallback
import io.github.kperczynski.libs.exception.ResourceMissingException
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory
import java.util.UUID

private val log = LoggerFactory.getLogger(ExposedPlantRepo::class.java)

@Singleton
class ExposedPlantRepo(
    private val database: Database,
    objectMapper: JsonMapper
) : PlantRepo, InitCallback {

    private val table = ExposedPlant(objectMapper)

    override fun onInit() {
        transaction(database) {
            log.info("Creating 'plants' table if it does not exist...")
            SchemaUtils.create(table)
        }
    }

    override suspend fun create(plant: Plant): Plant {
        return suspendTransaction(database) {
            val createdRecord = table.insert {
                it[externalId] = plant.externalId
                it[createdAt] = plant.createdAt
                it[status] = plant.status.name
                it[identifications] = plant.identifications
                it[displayName] = plant.displayName
                it[species] = plant.species
                it[cultivar] = plant.cultivar
                it[healthStatus] = plant.healthStatus
                it[careInstructions] = plant.careInstructions
                it[difficulty] = plant.difficulty.name
                it[toxicity] = plant.toxicity
                it[images] = plant.images
                it[tempIdentityId] = plant.tempIdentityId
                it[lastWatered] = plant.lastWatered?.name
                it[soilMoisture] = plant.soilMoisture?.name
                it[repottedSinceBuying] = plant.repottedSinceBuying?.name
                it[lightLocation] = plant.lightLocation?.name
                it[nearHeatOrAc] = plant.nearHeatOrAc?.name
                it[lastWateringDate] = plant.lastWateringDate
                it[lastFertilizationDate] = plant.lastFertilizationDate
                it[lastRepottingDate] = plant.lastRepottingDate
                it[lastStatusUpdateDate] = plant.lastStatusUpdateDate
            }
            plant.copy(id = createdRecord[table.id].value)
        }
    }

    override suspend fun find(id: UInt): Plant {
        return suspendTransaction(database) {
            val plant = table.selectAll()
                .where { table.id eq id }
                .map { toPlant(it) }
                .singleOrNull()
                ?: throw ResourceMissingException(Plant::class.java, id)

            plant
        }
    }

    override suspend fun findAll(): List<Plant> {
        return suspendTransaction(database) {
            table.selectAll()
                .map { toPlant(it) }
        }
    }

    override suspend fun update(plant: Plant) {
        suspendTransaction(database) {
            table.update({ table.id eq plant.id }) {
                it[status] = plant.status.name
                it[identifications] = plant.identifications
                it[displayName] = plant.displayName
                it[species] = plant.species
                it[cultivar] = plant.cultivar
                it[healthStatus] = plant.healthStatus
                it[careInstructions] = plant.careInstructions
                it[difficulty] = plant.difficulty.name
                it[toxicity] = plant.toxicity
                it[images] = plant.images
                it[tempIdentityId] = plant.tempIdentityId
                it[lastWatered] = plant.lastWatered?.name
                it[soilMoisture] = plant.soilMoisture?.name
                it[repottedSinceBuying] = plant.repottedSinceBuying?.name
                it[lightLocation] = plant.lightLocation?.name
                it[nearHeatOrAc] = plant.nearHeatOrAc?.name
                it[lastWateringDate] = plant.lastWateringDate
                it[lastFertilizationDate] = plant.lastFertilizationDate
                it[lastRepottingDate] = plant.lastRepottingDate
                it[lastStatusUpdateDate] = plant.lastStatusUpdateDate
            }
        }
    }

    override suspend fun delete(id: UInt) {
        suspendTransaction(database) {
            table.deleteWhere { table.id.eq(id) }
        }
    }

    override suspend fun findByTempIdentityId(tempIdentityId: UUID): Plant? {
        return suspendTransaction(database) {
            table.selectAll()
                .where { table.tempIdentityId eq tempIdentityId }
                .map { toPlant(it) }
                .singleOrNull()
        }
    }

    private fun toPlant(row: ResultRow): Plant {
        return Plant(
            id = row[table.id].value,
            externalId = row[table.externalId],
            createdAt = row[table.createdAt],
            status = PlantStatus.valueOf(row[table.status]),
            identifications = row[table.identifications],
            displayName = row[table.displayName],
            species = row[table.species],
            cultivar = row[table.cultivar],
            healthStatus = row[table.healthStatus],
            careInstructions = row[table.careInstructions],
            difficulty = PlantDifficulty.valueOf(row[table.difficulty]),
            toxicity = row[table.toxicity],
            images = row[table.images],
            tempIdentityId = row[table.tempIdentityId],
            lastWatered = row[table.lastWatered]?.let { LastWateredOption.valueOf(it) },
            soilMoisture = row[table.soilMoisture]?.let { SoilMoistureOption.valueOf(it) },
            repottedSinceBuying = row[table.repottedSinceBuying]?.let { YesNoOption.valueOf(it) },
            lightLocation = row[table.lightLocation]?.let { LightLocationOption.valueOf(it) },
            nearHeatOrAc = row[table.nearHeatOrAc]?.let { YesNoOption.valueOf(it) },
            lastWateringDate = row[table.lastWateringDate],
            lastFertilizationDate = row[table.lastFertilizationDate],
            lastRepottingDate = row[table.lastRepottingDate],
            lastStatusUpdateDate = row[table.lastStatusUpdateDate]
        )
    }
}
