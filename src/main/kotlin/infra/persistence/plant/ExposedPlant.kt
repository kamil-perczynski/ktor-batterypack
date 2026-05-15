package io.github.kperczynski.infra.persistence.plant

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import io.github.kperczynski.domain.plant.model.PlantCareInstructions
import io.github.kperczynski.domain.plant.model.PlantHealthStatus
import io.github.kperczynski.domain.plant.model.PlantIdentificationItem
import io.github.kperczynski.domain.plant.model.PlantImageInfo
import io.github.kperczynski.domain.plant.model.PlantToxicity
import org.jetbrains.exposed.v1.core.dao.id.UIntIdTable
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.json.jsonb

class ExposedPlant(objectMapper: JsonMapper) : UIntIdTable(name = "plants") {
    val externalId = javaUUID("external_id").uniqueIndex()
    val createdAt = timestamp("created_at")
    val status = varchar("status", length = 50)
    val identifications = jsonb<List<PlantIdentificationItem>>(
        "identifications",
        { objectMapper.writeValueAsString(it) },
        { objectMapper.readValue(it) }
    )
    val displayName = varchar("display_name", length = 255).nullable()
    val species = varchar("species", length = 255).nullable()
    val cultivar = varchar("cultivar", length = 255).nullable()
    val healthStatus = jsonb<PlantHealthStatus>(
        "health_status",
        { objectMapper.writeValueAsString(it) },
        { objectMapper.readValue(it) }
    )
    val careInstructions = jsonb<PlantCareInstructions>(
        "care_instructions",
        { objectMapper.writeValueAsString(it) },
        { objectMapper.readValue(it) }
    )
    val difficulty = varchar("difficulty", length = 50)
    val toxicity = jsonb<PlantToxicity>(
        "toxicity",
        { objectMapper.writeValueAsString(it) },
        { objectMapper.readValue(it) }
    )
    val images = jsonb<List<PlantImageInfo>>(
        "images",
        { objectMapper.writeValueAsString(it) },
        { objectMapper.readValue(it) }
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
