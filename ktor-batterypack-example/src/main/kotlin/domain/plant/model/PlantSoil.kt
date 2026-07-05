package io.github.kperczynski.domain.plant.model

import io.github.kperczynski.domain.plant.model.enums.SoilType

data class PlantSoil(
    val type: SoilType,
    val details: String? = null
)
