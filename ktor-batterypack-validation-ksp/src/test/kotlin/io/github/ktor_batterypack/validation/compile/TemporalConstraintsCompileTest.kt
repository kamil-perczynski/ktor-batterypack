package io.github.ktor_batterypack.validation.compile

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

class TemporalConstraintsCompileTest {

    companion object {
        private val compiler = ValidationProcessorCompiler()
        private val jsonMapper = JsonMapper.builder().build()

        private val result: JvmCompilationResult by lazy {
            compiler.compile(listOf(MODEL, SCENARIOS))
        }

        private val MODEL = SourceFile.kotlin(
            "Shift.kt",
            """
            package fixture.temporal

            import io.github.ktor_batterypack.annotation.JsonValidator
            import io.github.ktor_batterypack.annotation.Validator
            import io.github.ktor_batterypack.validation.ValidationParamType
            import io.github.ktor_batterypack.validation.ValidationResult
            import jakarta.validation.constraints.FutureOrPresent
            import jakarta.validation.constraints.Past
            import tools.jackson.databind.JsonNode
            import java.time.LocalDate
            import java.time.LocalTime
            import java.time.MonthDay
            import java.time.Year
            import java.util.Date

            data class Shift(
                @field:Past val startedAt: LocalTime,
                @field:Past val hiredIn: Year,
                @field:Past val signedAt: Date,
                @field:FutureOrPresent val anniversary: MonthDay,
            )

            data class Leave(@field:FutureOrPresent val from: LocalDate)

            @Validator
            interface ShiftValidator {
                fun validateShift(shift: Shift): ValidationResult<Shift>
            }

            @JsonValidator
            interface JsonLeaveValidator {
                @ValidationParamType(Leave::class)
                fun validateLeave(leave: JsonNode): ValidationResult<JsonNode>
            }
            """
        )

        private val SCENARIOS = SourceFile.kotlin(
            "Scenarios.kt",
            """
            package fixture.temporal

            import tools.jackson.databind.json.JsonMapper
            import java.time.Clock
            import java.time.Instant
            import java.time.LocalTime
            import java.time.MonthDay
            import java.time.Year
            import java.time.ZoneOffset
            import java.util.Date

            object Scenarios {

                private val CLOCK = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC)

                @JvmStatic
                fun typedValid(): Any {
                    return ShiftValidatorImpl(CLOCK).validateShift(
                        Shift(LocalTime.of(11, 0), Year.of(2025), Date.from(Instant.parse("2026-06-15T11:00:00Z")), MonthDay.of(6, 15))
                    )
                }

                @JvmStatic
                fun typedInvalid(): Any {
                    return ShiftValidatorImpl(CLOCK).validateShift(
                        Shift(LocalTime.of(13, 0), Year.of(2026), Date.from(Instant.parse("2026-06-15T12:00:00Z")), MonthDay.of(6, 14))
                    )
                }

                @JvmStatic
                fun json(json: String): Any {
                    return JsonLeaveValidatorImpl(CLOCK).validateLeave(JsonMapper.builder().build().readTree(json))
                }
            }
            """
        )
    }

    @Test
    fun `typed validator accepts temporals relative to the injected clock`() {
        assertThat(result.exitCode).withFailMessage(result.messages).isEqualTo(ExitCode.OK)
        assertThat(compiler.errorsJson(compiler.invokeStatic(result, "fixture.temporal.Scenarios", "typedValid"))).isNull()
    }

    @Test
    fun `typed validator rejects temporals relative to the injected clock`() {
        val errors = readTree(compiler.errorsJson(compiler.invokeStatic(result, "fixture.temporal.Scenarios", "typedInvalid")))

        assertThat(errors.at("/startedAt/0/constraint").asString()).isEqualTo("Past")
        assertThat(errors.at("/hiredIn/0/constraint").asString()).isEqualTo("Past")
        assertThat(errors.at("/signedAt/0/constraint").asString()).isEqualTo("Past")
        assertThat(errors.at("/anniversary/0/constraint").asString()).isEqualTo("FutureOrPresent")
    }

    @Test
    fun `json validator compares with the injected clock`() {
        assertThat(compiler.errorsJson(compiler.invokeStatic(result, "fixture.temporal.Scenarios", "json", """{"from": "2026-06-15"}""")))
            .isNull()

        val errors = readTree(compiler.errorsJson(compiler.invokeStatic(result, "fixture.temporal.Scenarios", "json", """{"from": "2026-06-14"}""")))
        assertThat(errors.at("/from/0/constraint").asString()).isEqualTo("FutureOrPresent")
    }

    private fun readTree(json: String?): JsonNode {
        return jsonMapper.readTree(json)
    }
}
