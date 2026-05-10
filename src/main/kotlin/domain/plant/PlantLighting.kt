package io.github.kperczynski.domain.plant

import io.github.kperczynski.domain.plant.enums.LightingType

data class PlantLighting(
    val type: LightingType,
    val details: String? = null
)
