package io.github.kperczynski.domain.plant

data class PlantCareInstructions(
    val watering: PlantMonthlySchedule,
    val soil: PlantSoil,
    val repotting: PlantRepotting,
    val lighting: PlantLighting,
    val fertilizing: PlantMonthlySchedule
)
