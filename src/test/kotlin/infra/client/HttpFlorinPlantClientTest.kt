package io.github.kperczynski.infra.client

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.github.kperczynski.domain.plant.*
import io.github.kperczynski.domain.plant.enums.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class HttpFlorinPlantClientTest {

    private val mockJsonResponse = """
    {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "createdAt": "2024-01-15T10:30:00Z",
        "status": "IDENTIFICATION",
        "identifications": [
            {
                "species": "Monstera deliciosa",
                "mostLikelyCommonName": "Swiss Cheese Plant",
                "cultivar": null,
                "commonNames": ["Swiss Cheese Plant", "Split-leaf Philodendron"],
                "speciesProbability": 0.95,
                "cultivarProbability": null
            }
        ],
        "displayName": "My Monstera",
        "species": "Monstera deliciosa",
        "cultivar": null,
        "healthStatus": {
            "condition": "HEALTHY",
            "cause": null,
            "details": "Plant appears healthy with vibrant leaves"
        },
        "careInstructions": {
            "watering": {
                "jan": 7, "feb": 7, "mar": 5, "apr": 5, "may": 4, "jun": 4,
                "jul": 3, "aug": 3, "sep": 4, "oct": 5, "nov": 7, "dec": 7,
                "details": "Water when top inch of soil is dry"
            },
            "soil": {
                "type": "AROID",
                "details": "Well-draining mix with perlite"
            },
            "repotting": {
                "intervalMonths": 24,
                "recommendedMonths": [3, 4, 5, 9, 10],
                "details": "Repot when roots outgrow pot"
            },
            "lighting": {
                "type": "BRIGHT_INDIRECT",
                "details": "Avoid direct sunlight"
            },
            "fertilizing": {
                "jan": 0, "feb": 0, "mar": 30, "apr": 30, "may": 30, "jun": 30,
                "jul": 30, "aug": 30, "sep": 30, "oct": 0, "nov": 0, "dec": 0,
                "details": "Use balanced liquid fertilizer monthly during growing season"
            }
        },
        "difficulty": "EASY",
        "toxicity": {
            "level": "MODERATE",
            "details": "Toxic to pets if ingested"
        },
        "images": [
            {
                "id": "660e8400-e29b-41d4-a716-446655440001",
                "url": "https://example.com/images/plant1.jpg"
            }
        ],
        "tempIdentityId": "770e8400-e29b-41d4-a716-446655440002"
    }
    """.trimIndent()

    @Test
    fun `should parse successful plant identification response`() {
        val objectMapper = ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        val plant = objectMapper.readValue(mockJsonResponse, Plant::class.java)

        assertThat(plant.id).isEqualTo(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
        assertThat(plant.status).isEqualTo(PlantStatus.IDENTIFICATION)
        assertThat(plant.identifications).hasSize(1)
        assertThat(plant.identifications[0].species).isEqualTo("Monstera deliciosa")
        assertThat(plant.identifications[0].speciesProbability).isEqualTo(0.95)
        assertThat(plant.healthStatus.condition).isEqualTo(PlantCondition.HEALTHY)
        assertThat(plant.careInstructions.watering.jan).isEqualTo(7)
        assertThat(plant.careInstructions.soil.type).isEqualTo(SoilType.AROID)
        assertThat(plant.difficulty).isEqualTo(PlantDifficulty.EASY)
        assertThat(plant.toxicity.level).isEqualTo(ToxicityLevel.MODERATE)
    }

    @Test
    fun `should handle plant identification exception`() {
        val exception = PlantIdentificationException("Test error message")
        assertThat(exception).isInstanceOf(RuntimeException::class.java)
        assertThat(exception.message).isEqualTo("Test error message")
    }
}
