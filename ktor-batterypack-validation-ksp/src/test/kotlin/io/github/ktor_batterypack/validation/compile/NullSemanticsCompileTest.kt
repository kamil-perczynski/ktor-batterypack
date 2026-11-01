package io.github.ktor_batterypack.validation.compile

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class NullSemanticsCompileTest {

    companion object {
        private val compiler = ValidationProcessorCompiler()

        private val result: JvmCompilationResult by lazy {
            compiler.compile(listOf(MODEL, SCENARIOS))
        }

        private val MODEL = SourceFile.kotlin(
            "Consent.kt",
            """
            package fixture.nulls

            import io.github.ktor_batterypack.annotation.JsonValidator
            import io.github.ktor_batterypack.annotation.Validator
            import io.github.ktor_batterypack.validation.ValidationParamType
            import io.github.ktor_batterypack.validation.ValidationResult
            import jakarta.validation.constraints.AssertFalse
            import jakarta.validation.constraints.AssertTrue
            import jakarta.validation.constraints.Digits
            import jakarta.validation.constraints.NotBlank
            import jakarta.validation.constraints.NotEmpty
            import jakarta.validation.constraints.NotNull
            import jakarta.validation.constraints.Null
            import jakarta.validation.constraints.Size
            import tools.jackson.databind.JsonNode
            import java.math.BigDecimal

            data class Consent(
                @field:NotNull
                val reference: String?,
                @field:NotBlank
                val note: String?,
                @field:NotEmpty
                val items: List<String>?,
                @field:Null
                val legacy: String?,
                @field:AssertTrue
                val accepted: Boolean,
                @field:AssertFalse
                val revoked: Boolean?,
                @field:Digits(integer = 3, fraction = 2)
                val amount: BigDecimal?,
                @field:Size(max = 2)
                val code: String?,
                val alias: @NotNull String?,
            )

            @Validator
            interface ConsentValidator {
                fun validateConsent(consent: Consent): ValidationResult<Consent>
            }

            @JsonValidator
            interface JsonConsentValidator {
                @ValidationParamType(Consent::class)
                fun validateConsent(consent: JsonNode): ValidationResult<JsonNode>
            }
            """
        )

        private val SCENARIOS = SourceFile.kotlin(
            "Scenarios.kt",
            """
            package fixture.nulls

            import tools.jackson.databind.json.JsonMapper
            import java.math.BigDecimal

            object Scenarios {

                @JvmStatic
                fun typedNulls(): Any {
                    return ConsentValidatorImpl().validateConsent(
                        Consent(null, null, null, null, true, null, null, null, null)
                    )
                }

                @JvmStatic
                fun typedValid(): Any {
                    return ConsentValidatorImpl().validateConsent(
                        Consent("r", "n", listOf("i"), null, true, false, BigDecimal("999.99"), "ab", "a")
                    )
                }

                @JvmStatic
                fun typedInvalid(): Any {
                    return ConsentValidatorImpl().validateConsent(
                        Consent("r", " ", emptyList(), "x", false, true, BigDecimal("1234.5"), "abc", "a")
                    )
                }

                @JvmStatic
                fun json(json: String): Any {
                    return JsonConsentValidatorImpl().validateConsent(JsonMapper.builder().build().readTree(json))
                }
            }
            """
        )

        private const val INVALID_ERRORS = """
            {
              "note": [{"constraint": "NotBlank"}],
              "items": {"errors": [%s], "items": []},
              "legacy": [{"constraint": "Null", "message": "Must be null"}],
              "accepted": [{"constraint": "AssertTrue", "message": "Must be true"}],
              "revoked": [{"constraint": "AssertFalse", "message": "Must be false"}],
              "amount": [{"constraint": "Digits", "message": "Must have at most 3 integer digits and 2 fraction digits"}],
              "code": [{"constraint": "Size", "message": "Must be shorter than max=2"}]
            }
        """
    }

    @Test
    fun `validators with null semantics compile`() {
        assertThat(result.exitCode).withFailMessage(result.messages).isEqualTo(ExitCode.OK)
    }

    @Test
    fun `typed validator rejects null only for null-rejecting constraints`() {
        val validationResult = compiler.invokeStatic(result, "fixture.nulls.Scenarios", "typedNulls")

        assertThat(readTree(compiler.errorsJson(validationResult))).isEqualTo(
            readTree(
                """
                {
                  "reference": [{"constraint": "NotNull", "message": "Must not be null"}],
                  "note": [{"constraint": "NotNull", "message": "Must not be null"}],
                  "items": [{"constraint": "NotNull", "message": "Must not be null"}],
                  "alias": [{"constraint": "NotNull", "message": "Must not be null"}]
                }
                """
            )
        )
    }

    @Test
    fun `typed validator accepts values within constraints`() {
        val validationResult = compiler.invokeStatic(result, "fixture.nulls.Scenarios", "typedValid")

        assertThat(compiler.errorsJson(validationResult)).isNull()
    }

    @Test
    fun `typed validator enforces Null, AssertTrue, AssertFalse and Digits`() {
        val validationResult = compiler.invokeStatic(result, "fixture.nulls.Scenarios", "typedInvalid")

        assertThat(readTree(compiler.errorsJson(validationResult))).isEqualTo(
            readTree(INVALID_ERRORS.format("""{"constraint": "NotEmpty"}"""))
        )
    }

    @Test
    fun `json validator requires null-rejecting and non-null properties without defaults`() {
        val validationResult = compiler.invokeStatic(result, "fixture.nulls.Scenarios", "json", """{"note": null}""")

        assertThat(readTree(compiler.errorsJson(validationResult))).isEqualTo(
            readTree(
                """
                {
                  "reference": [{"constraint": "NotNull", "message": "Must not be null"}],
                  "note": [{"constraint": "NotNull", "message": "Must not be null"}],
                  "items": [{"constraint": "NotNull", "message": "Must not be null"}],
                  "accepted": [{"constraint": "NotNull", "message": "Must not be null"}],
                  "alias": [{"constraint": "NotNull", "message": "Must not be null"}]
                }
                """
            )
        )
    }

    @Test
    fun `json validator accepts values within constraints`() {
        val validationResult = compiler.invokeStatic(
            result,
            "fixture.nulls.Scenarios",
            "json",
            """{"reference": "r", "note": "n", "items": ["i"], "accepted": true, "amount": 999.99, "alias": "a"}"""
        )

        assertThat(compiler.errorsJson(validationResult)).isNull()
    }

    @Test
    fun `json validator enforces Null, AssertTrue, AssertFalse and Digits`() {
        val validationResult = compiler.invokeStatic(
            result,
            "fixture.nulls.Scenarios",
            "json",
            """
            {
              "reference": "r",
              "note": " ",
              "items": [],
              "legacy": "x",
              "accepted": false,
              "revoked": true,
              "amount": 1234.5,
              "code": "abc",
              "alias": "a"
            }
            """
        )

        assertThat(readTree(compiler.errorsJson(validationResult))).isEqualTo(
            readTree(INVALID_ERRORS.format("""{"constraint": "NotEmpty", "message": "Must not be empty array"}"""))
        )
    }

    private fun readTree(json: String?): Any? {
        if (json == null) {
            return null
        }
        return JsonMapper.builder().build().readTree(json)
    }

}
