package io.github.kperczynski.domain.plant

import io.github.kperczynski.domain.plant.enums.SoilType

data class PlantSoil(
    val type: SoilType,
    val details: String? = null
)
