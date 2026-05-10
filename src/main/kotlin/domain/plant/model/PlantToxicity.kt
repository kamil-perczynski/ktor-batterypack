package io.github.kperczynski.domain.plant.model

import io.github.kperczynski.domain.plant.model.enums.ToxicityLevel

data class PlantToxicity(
    val level: ToxicityLevel,
    val details: String? = null
)
