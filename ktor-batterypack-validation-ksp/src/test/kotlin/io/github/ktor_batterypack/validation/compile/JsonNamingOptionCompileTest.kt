package io.github.ktor_batterypack.validation.compile

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class JsonNamingOptionCompileTest {

    companion object {
        private val SOURCE = SourceFile.kotlin(
            "Profile.kt",
            """
            package fixture.naming

            import com.fasterxml.jackson.annotation.JsonProperty
            import io.github.ktor_batterypack.annotation.JsonValidator
            import io.github.ktor_batterypack.validation.ValidationParamType
            import io.github.ktor_batterypack.validation.ValidationResult
            import jakarta.validation.constraints.NotBlank
            import jakarta.validation.constraints.Size
            import tools.jackson.databind.JsonNode
            import tools.jackson.databind.json.JsonMapper

            data class Profile(
                @field:NotBlank
                val displayName: String,
                @JsonProperty("ID")
                @field:NotBlank
                val userId: String,
                @field:Size(max = 5)
                val homePage: String?,
            )

            @JsonValidator
            interface ProfileValidator {
                @ValidationParamType(Profile::class)
                fun validateProfile(profile: JsonNode): ValidationResult<JsonNode>
            }

            object Scenarios {
                @JvmStatic
                fun validate(json: String): Any {
                    return ProfileValidatorImpl().validateProfile(JsonMapper.builder().build().readTree(json))
                }
            }
            """
        )
    }

    private val compiler = ValidationProcessorCompiler()

    @Test
    fun `naming strategy option translates property names unless json property is explicit`() {
        val result = compiler.compile(listOf(SOURCE), mapOf("ktor.validation.json.naming" to "SNAKE_CASE"))
        assertThat(result.exitCode).withFailMessage(result.messages).isEqualTo(ExitCode.OK)

        val missing = compiler.invokeStatic(result, "fixture.naming.Scenarios", "validate", "{}")
        val invalid = compiler.invokeStatic(
            result,
            "fixture.naming.Scenarios",
            "validate",
            """{"display_name": " ", "ID": " ", "home_page": "https://example.com"}"""
        )
        val valid = compiler.invokeStatic(
            result,
            "fixture.naming.Scenarios",
            "validate",
            """{"display_name": "Ann", "ID": "42", "home_page": null}"""
        )

        assertThat(readTree(compiler.errorsJson(missing))).isEqualTo(
            readTree(
                """
                {
                  "display_name": [{"constraint": "NotNull", "message": "Must not be null"}],
                  "ID": [{"constraint": "NotNull", "message": "Must not be null"}]
                }
                """
            )
        )
        assertThat(readTree(compiler.errorsJson(invalid))).isEqualTo(
            readTree(
                """
                {
                  "display_name": [{"constraint": "NotBlank"}],
                  "ID": [{"constraint": "NotBlank"}],
                  "home_page": [{"constraint": "Size", "message": "Must be shorter than max=5"}]
                }
                """
            )
        )
        assertThat(compiler.errorsJson(valid)).isNull()
    }

    @Test
    fun `unsupported naming strategy fails the compilation`() {
        val result = compiler.compile(listOf(SOURCE), mapOf("ktor.validation.json.naming" to "SCREAMING_CASE"))

        assertThat(result.exitCode).isNotEqualTo(ExitCode.OK)
        assertThat(result.messages).contains("Unsupported ktor.validation.json.naming 'SCREAMING_CASE'")
    }

    private fun readTree(json: String?): Any? {
        if (json == null) {
            return null
        }
        return JsonMapper.builder().build().readTree(json)
    }

}
