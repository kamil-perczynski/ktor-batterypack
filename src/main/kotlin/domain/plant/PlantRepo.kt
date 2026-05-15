package io.github.kperczynski.domain.plant

import io.github.kperczynski.domain.plant.model.Plant
import java.util.UUID

interface PlantRepo {

    suspend fun create(plant: Plant): Plant

    suspend fun find(id: UInt): Plant

    suspend fun findAll(): List<Plant>

    suspend fun update(plant: Plant)

    suspend fun delete(id: UInt)

    suspend fun findByTempIdentityId(tempIdentityId: UUID): Plant?

}
