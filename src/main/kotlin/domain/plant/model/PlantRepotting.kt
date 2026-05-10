package io.github.kperczynski.domain.plant.model

data class PlantRepotting(
    val intervalMonths: Int,
    val recommendedMonths: List<Int>,
    val details: String? = null
)
