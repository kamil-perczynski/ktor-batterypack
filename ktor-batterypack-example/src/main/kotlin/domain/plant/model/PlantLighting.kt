package io.github.kperczynski.domain.plant.model

import io.github.kperczynski.domain.plant.model.enums.LightingType

data class PlantLighting(
    val type: LightingType,
    val details: String? = null
)
