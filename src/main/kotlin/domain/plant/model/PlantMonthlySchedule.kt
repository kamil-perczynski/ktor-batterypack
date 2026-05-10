package io.github.kperczynski.domain.plant.model

data class PlantMonthlySchedule(
    val jan: Int,
    val feb: Int,
    val mar: Int,
    val apr: Int,
    val may: Int,
    val jun: Int,
    val jul: Int,
    val aug: Int,
    val sep: Int,
    val oct: Int,
    val nov: Int,
    val dec: Int,
    val details: String? = null
)
