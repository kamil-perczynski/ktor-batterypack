package io.github.kperczynski.domain.plant

import io.github.kperczynski.domain.plant.enums.ToxicityLevel

data class PlantToxicity(
    val level: ToxicityLevel,
    val details: String? = null
)
