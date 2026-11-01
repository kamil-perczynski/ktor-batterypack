package io.github.ktor_batterypack.validation.compile

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class JsonValidatorCompileTest {

    companion object {
        private val compiler = ValidationProcessorCompiler()

        private val result: JvmCompilationResult by lazy {
            compiler.compile(listOf(MODEL, VALIDATOR, SCENARIOS))
        }

        private val MODEL = SourceFile.kotlin(
            "Event.kt",
            """
            package fixture.json

            import jakarta.validation.constraints.DecimalMax
            import jakarta.validation.constraints.NotBlank
            import jakarta.validation.constraints.Size
            import java.math.BigDecimal
            import java.time.Instant
            import java.time.LocalDate
            import java.time.LocalDateTime
            import java.time.LocalTime
            import java.time.OffsetDateTime
            import java.util.UUID

            enum class Status { ACTIVE, BLOCKED }

            data class Venue(
                @field:NotBlank
                val name: String,
            )

            data class Event(
                @field:NotBlank
                val title: String,
                val count: Int,
                val big: Long?,
                val ratio: Float?,
                val score: Double?,
                @field:DecimalMax("100")
                val amount: BigDecimal?,
                val enabled: Boolean?,
                val id: UUID,
                val day: LocalDate?,
                val startsAt: LocalDateTime?,
                val opensAt: LocalTime?,
                val offset: OffsetDateTime?,
                val instant: Instant?,
                val status: Status?,
                val venue: Venue?,
                val stamps: List<OffsetDateTime>?,
                val venues: List<Venue>?,
                @field:Size(max = 2)
                val labels: List<String>?,
            )
            """
        )

        private val VALIDATOR = SourceFile.kotlin(
            "EventValidator.kt",
            """
            package fixture.json

            import io.github.ktor_batterypack.annotation.JsonValidator
            import io.github.ktor_batterypack.validation.ValidationParamType
            import io.github.ktor_batterypack.validation.ValidationResult
            import tools.jackson.databind.JsonNode

            @JsonValidator
            interface EventValidator {
                @ValidationParamType(Event::class)
                fun validateEvent(event: JsonNode): ValidationResult<JsonNode>
            }
            """
        )

        private val SCENARIOS = SourceFile.kotlin(
            "Scenarios.kt",
            """
            package fixture.json

            import tools.jackson.databind.json.JsonMapper

            object Scenarios {

                @JvmStatic
                fun validate(json: String): Any {
                    return EventValidatorImpl().validateEvent(JsonMapper.builder().build().readTree(json))
                }
            }
            """
        )
    }

    @Test
    fun `json validator for every supported scalar, enum, nested and list property compiles`() {
        assertThat(result.exitCode).withFailMessage(result.messages).isEqualTo(ExitCode.OK)
        assertThat(compiler.generatedSource(result, "EventValidatorImpl.kt")).contains("class EventValidatorImpl")
    }

    @Test
    fun `json validator accepts a fully populated payload`() {
        val validationResult = validate(
            """
            {
              "title": "Concert",
              "count": 3,
              "big": 99999999999,
              "ratio": 0.5,
              "score": 1.25,
              "amount": "100",
              "enabled": true,
              "id": "8a3c4f0e-3d0b-4a57-9d8a-2b0c8e1f4a11",
              "day": "2024-02-29",
              "startsAt": "2024-02-29T20:00",
              "opensAt": "19:30:00",
              "offset": "2024-02-29T20:00:00+01:00",
              "instant": "2024-02-29T19:00:00Z",
              "status": "ACTIVE",
              "venue": {"name": "Hall"},
              "stamps": ["2024-02-29T20:00:00Z"],
              "venues": [{"name": "Hall"}],
              "labels": ["a", "b"]
            }
            """
        )

        assertThat(compiler.errorsJson(validationResult)).isNull()
    }

    @Test
    fun `json validator accepts explicit null on nullable properties`() {
        val validationResult = validate(
            """
            {
              "title": "Concert",
              "count": 3,
              "big": null,
              "ratio": null,
              "score": null,
              "amount": null,
              "enabled": null,
              "id": "8a3c4f0e-3d0b-4a57-9d8a-2b0c8e1f4a11",
              "day": null,
              "startsAt": null,
              "opensAt": null,
              "offset": null,
              "instant": null,
              "status": null,
              "venue": null,
              "stamps": null,
              "venues": null,
              "labels": null
            }
            """
        )

        assertThat(compiler.errorsJson(validationResult)).isNull()
    }

    @Test
    fun `json validator reports malformed values instead of throwing`() {
        val validationResult = validate(
            """
            {
              "title": " ",
              "count": 1.5,
              "big": "1e3",
              "amount": 100.5,
              "id": "not-a-uuid",
              "day": "2024-13-45",
              "offset": "2024-02-30T10:00:00Z",
              "status": "UNKNOWN",
              "venue": {"name": ""},
              "stamps": ["2024-02-29T25:00:00Z"],
              "labels": ["a", "b", "c"]
            }
            """
        )

        assertThat(readTree(compiler.errorsJson(validationResult))).isEqualTo(
            readTree(
                """
                {
                  "title": [{"constraint": "NotBlank"}],
                  "count": [{"constraint": "Type", "message": "Expected integer"}],
                  "big": [{"constraint": "Type", "message": "Expected integer"}],
                  "amount": [{"constraint": "DecimalMax", "message": "Must be less than or equal to 100"}],
                  "id": [{"constraint": "Format", "message": "Must be a valid UUID"}],
                  "day": [{"constraint": "Format", "message": "Must be a valid local date"}],
                  "offset": [{"constraint": "Format", "message": "Must be a valid offset date-time"}],
                  "status": [{"constraint": "EnumValues", "message": "Allowed values are: [ACTIVE, BLOCKED]"}],
                  "venue": {"name": [{"constraint": "NotBlank"}]},
                  "stamps": {"errors": [], "items": [{"$": [{"constraint": "Format", "message": "Must be a valid offset date-time"}]}]},
                  "labels": {"errors": [{"constraint": "Size", "message": "Must have size between [0, 2]"}], "items": [null, null, null]}
                }
                """
            )
        )
    }

    private fun validate(json: String): Any? {
        return compiler.invokeStatic(result, "fixture.json.Scenarios", "validate", json)
    }

    private fun readTree(json: String?): Any? {
        if (json == null) {
            return null
        }
        return JsonMapper.builder().build().readTree(json)
    }

}
