package io.github.kperczynski.domain

import io.github.kperczynski.domain.plant.model.Plant
import io.github.kperczynski.domain.plant.model.PlantCareInstructions
import io.github.kperczynski.domain.plant.model.PlantHealthStatus
import io.github.kperczynski.domain.plant.model.PlantIdentificationItem
import io.github.kperczynski.domain.plant.model.PlantImageInfo
import io.github.kperczynski.domain.plant.model.PlantLighting
import io.github.kperczynski.domain.plant.model.PlantMonthlySchedule
import io.github.kperczynski.domain.plant.model.PlantRepotting
import io.github.kperczynski.domain.plant.model.PlantSoil
import io.github.kperczynski.domain.plant.model.PlantToxicity
import io.github.kperczynski.domain.plant.model.enums.LightingType
import io.github.kperczynski.domain.plant.model.enums.PlantCondition
import io.github.kperczynski.domain.plant.model.enums.PlantDifficulty
import io.github.kperczynski.domain.plant.model.enums.PlantStatus
import io.github.kperczynski.domain.plant.model.enums.SoilType
import io.github.kperczynski.domain.plant.model.enums.ToxicityLevel
import java.time.Instant
import java.util.UUID

fun somePlant(): Plant = Plant(
    id = 1u,
    externalId = UUID.fromString("8251fb55-a2ae-41fd-8bc3-9dc673b260c8"),
    createdAt = Instant.parse("2026-05-10T17:02:52.669956Z"),
    status = PlantStatus.IDENTIFICATION,
    identifications = somePlantIdentifications(),
    displayName = null,
    species = null,
    cultivar = null,
    healthStatus = somePlantHealthStatus(),
    careInstructions = somePlantCareInstructions(),
    difficulty = PlantDifficulty.BEGINNER,
    toxicity = somePlantToxicity(),
    images = somePlantImages(),
    tempIdentityId = UUID.fromString("e77d40fa-729c-4475-94fb-406d845d6ef6"),
    lastWatered = null,
    soilMoisture = null,
    repottedSinceBuying = null,
    lightLocation = null,
    nearHeatOrAc = null,
    lastWateringDate = null,
    lastFertilizationDate = null,
    lastRepottingDate = null,
    lastStatusUpdateDate = null
)

private fun somePlantIdentifications(): List<PlantIdentificationItem> = listOf(
    PlantIdentificationItem(
        species = "Dracaena trifasciata",
        mostLikelyCommonName = "Snake Plant Moonshine",
        cultivar = "Moonshine",
        commonNames = listOf("Snake plant", "Mother in laws tongue"),
        speciesProbability = 0.98,
        cultivarProbability = 0.95
    ),
    PlantIdentificationItem(
        species = "Dracaena trifasciata",
        mostLikelyCommonName = "Snake Plant Silver Laurentii",
        cultivar = "Silver Laurentii",
        commonNames = listOf("Snake plant"),
        speciesProbability = 0.98,
        cultivarProbability = 0.6
    ),
    PlantIdentificationItem(
        species = "Dracaena masoniana",
        mostLikelyCommonName = "Whale Fin Snake Plant",
        cultivar = null,
        commonNames = listOf("Whale fin"),
        speciesProbability = 0.75,
        cultivarProbability = null
    )
)

private fun somePlantHealthStatus(): PlantHealthStatus = PlantHealthStatus(
    condition = PlantCondition.HEALTHY,
    cause = null,
    details = "The plant appears to be in good condition. The leaves are firm and upright, which is characteristic of the Moonshine variety."
)

private fun somePlantCareInstructions(): PlantCareInstructions = PlantCareInstructions(
    watering = someWateringSchedule(),
    soil = somePlantSoil(),
    repotting = somePlantRepotting(),
    lighting = somePlantLighting(),
    fertilizing = someFertilizingSchedule()
)

private fun someWateringSchedule(): PlantMonthlySchedule = PlantMonthlySchedule(
    jan = 30,
    feb = 28,
    mar = 25,
    apr = 20,
    may = 15,
    jun = 14,
    jul = 14,
    aug = 15,
    sep = 20,
    oct = 25,
    nov = 30,
    dec = 35,
    details = "Allow the soil to dry out completely between waterings. During periods of lower daylight, reduce watering frequency significantly to prevent root rot."
)

private fun somePlantSoil(): PlantSoil = PlantSoil(
    type = SoilType.CACTUS,
    details = "Use a well-draining succulent or cactus mix to ensure excess water does not linger around the roots."
)

private fun somePlantRepotting(): PlantRepotting = PlantRepotting(
    intervalMonths = 24,
    recommendedMonths = listOf(5, 6),
    details = "Repot only when the plant has become pot-bound, as they prefer to be slightly crowded."
)

private fun somePlantLighting(): PlantLighting = PlantLighting(
    type = LightingType.BRIGHT_INDIRECT,
    details = "Thrives in bright indirect light, but can tolerate lower light conditions. Avoid harsh, direct afternoon sun which may scorch the leaves."
)

private fun someFertilizingSchedule(): PlantMonthlySchedule = PlantMonthlySchedule(
    jan = 0,
    feb = 0,
    mar = 60,
    apr = 0,
    may = 60,
    jun = 0,
    jul = 60,
    aug = 0,
    sep = 0,
    oct = 0,
    nov = 0,
    dec = 0,
    details = "Fertilize sparingly during the active growing season (spring and summer) with a balanced liquid fertilizer diluted to half strength."
)

private fun somePlantToxicity(): PlantToxicity = PlantToxicity(
    level = ToxicityLevel.MODERATE,
    details = "Toxic if ingested. Can cause nausea, vomiting, and diarrhea in humans and pets due to the presence of saponins."
)

private fun somePlantImages(): List<PlantImageInfo> = listOf(
    PlantImageInfo(
        id = UUID.fromString("831aaf1a-02ad-44d1-a8b4-82d496ac0a60"),
        url = "/api/plant-images/831aaf1a-02ad-44d1-a8b4-82d496ac0a60/Sansevieria-Moonshine-1.webp"
    )
)

