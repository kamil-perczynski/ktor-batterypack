package io.github.kperczynski.domain.plant

import io.github.kperczynski.domain.plant.enums.PlantCondition
import io.github.kperczynski.domain.plant.enums.PlantConditionCause

data class PlantHealthStatus(
    val condition: PlantCondition,
    val cause: PlantConditionCause? = null,
    val details: String? = null
)
