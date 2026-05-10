package io.github.kperczynski.domain.plant

import io.github.kperczynski.domain.plant.enums.PlantDifficulty
import io.github.kperczynski.domain.plant.enums.PlantStatus
import io.github.kperczynski.domain.plant.enums.LastWateredOption
import io.github.kperczynski.domain.plant.enums.SoilMoistureOption
import io.github.kperczynski.domain.plant.enums.YesNoOption
import io.github.kperczynski.domain.plant.enums.LightLocationOption
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class Plant(
    val id: UUID,
    val createdAt: Instant,
    val status: PlantStatus,
    val identifications: List<PlantIdentificationItem>,
    val displayName: String? = null,
    val species: String? = null,
    val cultivar: String? = null,
    val healthStatus: PlantHealthStatus,
    val careInstructions: PlantCareInstructions,
    val difficulty: PlantDifficulty,
    val toxicity: PlantToxicity,
    val images: List<PlantImageInfo>,
    val tempIdentityId: UUID? = null,
    val lastWatered: LastWateredOption? = null,
    val soilMoisture: SoilMoistureOption? = null,
    val repottedSinceBuying: YesNoOption? = null,
    val lightLocation: LightLocationOption? = null,
    val nearHeatOrAc: YesNoOption? = null,
    val lastWateringDate: LocalDate? = null,
    val lastFertilizationDate: LocalDate? = null,
    val lastRepottingDate: LocalDate? = null,
    val lastStatusUpdateDate: LocalDate? = null
)
