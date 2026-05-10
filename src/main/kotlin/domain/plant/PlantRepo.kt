package io.github.kperczynski.domain.plant

import io.github.kperczynski.domain.plant.model.Plant
import java.util.UUID

interface PlantRepo {

    fun create(plant: Plant): Plant

    fun find(id: UInt): Plant

    fun findAll(): List<Plant>

    fun update(plant: Plant)

    fun delete(id: UInt)

    fun findByTempIdentityId(tempIdentityId: UUID): Plant?

}
