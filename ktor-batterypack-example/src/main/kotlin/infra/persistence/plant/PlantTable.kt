package io.github.kperczynski.infra.persistence.plant

import tools.jackson.module.kotlin.readValue
import io.github.kperczynski.domain.plant.model.PlantCareInstructions
import io.github.kperczynski.domain.plant.model.PlantHealthStatus
import io.github.kperczynski.domain.plant.model.PlantIdentificationItem
import io.github.kperczynski.domain.plant.model.PlantImageInfo
import io.github.kperczynski.domain.plant.model.PlantToxicity
import io.github.kperczynski.infra.persistence.exposedJsonMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UIntIdTable
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.dao.UIntEntity
import org.jetbrains.exposed.v1.dao.UIntEntityClass
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.json.jsonb

object PlantTable : UIntIdTable(name = "plants") {
    val externalId = javaUUID("external_id").uniqueIndex()
    val createdAt = timestamp("created_at")
    val status = varchar("status", length = 50)
    val identifications = jsonb<List<PlantIdentificationItem>>(
        "identifications",
        { exposedJsonMapper.writeValueAsString(it) },
        { exposedJsonMapper.readValue(it) }
    )
    val displayName = varchar("display_name", length = 255).nullable()
    val species = varchar("species", length = 255).nullable()
    val cultivar = varchar("cultivar", length = 255).nullable()
    val healthStatus = jsonb<PlantHealthStatus>(
        "health_status",
        { exposedJsonMapper.writeValueAsString(it) },
        { exposedJsonMapper.readValue(it) }
    )
    val careInstructions = jsonb<PlantCareInstructions>(
        "care_instructions",
        { exposedJsonMapper.writeValueAsString(it) },
        { exposedJsonMapper.readValue(it) }
    )
    val difficulty = varchar("difficulty", length = 50)
    val toxicity = jsonb<PlantToxicity>(
        "toxicity",
        { exposedJsonMapper.writeValueAsString(it) },
        { exposedJsonMapper.readValue(it) }
    )
    val images = jsonb<List<PlantImageInfo>>(
        "images",
        { exposedJsonMapper.writeValueAsString(it) },
        { exposedJsonMapper.readValue(it) }
    )
    val tempIdentityId = javaUUID("temp_identity_id").nullable()
    val lastWatered = varchar("last_watered", length = 50).nullable()
    val soilMoisture = varchar("soil_moisture", length = 50).nullable()
    val repottedSinceBuying = varchar("repotted_since_buying", length = 50).nullable()
    val lightLocation = varchar("light_location", length = 50).nullable()
    val nearHeatOrAc = varchar("near_heat_or_ac", length = 50).nullable()
    val lastWateringDate = date("last_watering_date").nullable()
    val lastFertilizationDate = date("last_fertilization_date").nullable()
    val lastRepottingDate = date("last_repotting_date").nullable()
    val lastStatusUpdateDate = date("last_status_update_date").nullable()
}

class PlantEntity(id: EntityID<UInt>) : UIntEntity(id) {
    companion object : UIntEntityClass<PlantEntity>(PlantTable)

    var externalId by PlantTable.externalId
    var createdAt by PlantTable.createdAt
    var status by PlantTable.status
    var identifications by PlantTable.identifications
    var displayName by PlantTable.displayName
    var species by PlantTable.species
    var cultivar by PlantTable.cultivar
    var healthStatus by PlantTable.healthStatus
    var careInstructions by PlantTable.careInstructions
    var difficulty by PlantTable.difficulty
    var toxicity by PlantTable.toxicity
    var images by PlantTable.images
    var tempIdentityId by PlantTable.tempIdentityId
    var lastWatered by PlantTable.lastWatered
    var soilMoisture by PlantTable.soilMoisture
    var repottedSinceBuying by PlantTable.repottedSinceBuying
    var lightLocation by PlantTable.lightLocation
    var nearHeatOrAc by PlantTable.nearHeatOrAc
    var lastWateringDate by PlantTable.lastWateringDate
    var lastFertilizationDate by PlantTable.lastFertilizationDate
    var lastRepottingDate by PlantTable.lastRepottingDate
    var lastStatusUpdateDate by PlantTable.lastStatusUpdateDate
}
