package io.github.kperczynski.domain.plant

data class PlantIdentificationItem(
    val species: String,
    val mostLikelyCommonName: String? = null,
    val cultivar: String? = null,
    val commonNames: List<String> = emptyList(),
    val speciesProbability: Double,
    val cultivarProbability: Double? = null
)
