package io.github.kperczynski.domain.plant.model

import io.github.kperczynski.domain.plant.model.enums.PlantCondition
import io.github.kperczynski.domain.plant.model.enums.PlantConditionCause

data class PlantHealthStatus(
    val condition: PlantCondition,
    val cause: PlantConditionCause? = null,
    val details: String? = null
)
