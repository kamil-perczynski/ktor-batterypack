package io.github.ktor_batterypack.validation.compile

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class CollectionConstraintsCompileTest {

    companion object {
        private val compiler = ValidationProcessorCompiler()

        private val result: JvmCompilationResult by lazy {
            compiler.compile(listOf(MODEL, SCENARIOS))
        }

        private val MODEL = SourceFile.kotlin(
            "Catalog.kt",
            """
            package fixture.collections

            import io.github.ktor_batterypack.annotation.JsonValidator
            import io.github.ktor_batterypack.annotation.Validator
            import io.github.ktor_batterypack.validation.ValidationParamType
            import io.github.ktor_batterypack.validation.ValidationResult
            import jakarta.validation.constraints.NotBlank
            import jakarta.validation.constraints.NotEmpty
            import jakarta.validation.constraints.NotNull
            import jakarta.validation.constraints.Size
            import tools.jackson.databind.JsonNode

            enum class Color { RED, GREEN }

            data class Catalog(
                val labels: Map<String, @NotBlank String>,
                @field:NotEmpty
                val attributes: Map<String, String>?,
                @field:Size(max = 1)
                val limits: Map<String, Int>?,
                val aliases: List<String?>,
                val colors: Map<String, Color>?,
                val codes: List<@NotNull String?>?,
            )

            @Validator
            interface CatalogValidator {
                fun validateCatalog(catalog: Catalog): ValidationResult<Catalog>
            }

            @JsonValidator
            interface JsonCatalogValidator {
                @ValidationParamType(Catalog::class)
                fun validateCatalog(catalog: JsonNode): ValidationResult<JsonNode>
            }
            """
        )

        private val SCENARIOS = SourceFile.kotlin(
            "Scenarios.kt",
            """
            package fixture.collections

            import tools.jackson.databind.json.JsonMapper

            object Scenarios {

                @JvmStatic
                fun typedValid(): Any {
                    return CatalogValidatorImpl().validateCatalog(
                        Catalog(mapOf("k" to "v"), mapOf("a" to "b"), null, listOf(null, "a"), null, null)
                    )
                }

                @JvmStatic
                fun typedInvalid(): Any {
                    return CatalogValidatorImpl().validateCatalog(
                        Catalog(
                            labels = mapOf("ok" to "x", "blank" to " "),
                            attributes = emptyMap(),
                            limits = mapOf("a" to 1, "b" to 2),
                            aliases = listOf("a", null),
                            colors = mapOf("c" to Color.RED),
                            codes = listOf("x", null),
                        )
                    )
                }

                @JvmStatic
                fun json(json: String): Any {
                    return JsonCatalogValidatorImpl().validateCatalog(JsonMapper.builder().build().readTree(json))
                }
            }
            """
        )
    }

    @Test
    fun `validators with map and nullable item constraints compile`() {
        assertThat(result.exitCode).withFailMessage(result.messages).isEqualTo(ExitCode.OK)
    }

    @Test
    fun `typed validator accepts valid maps and nullable list items`() {
        val validationResult = compiler.invokeStatic(result, "fixture.collections.Scenarios", "typedValid")

        assertThat(compiler.errorsJson(validationResult)).isNull()
    }

    @Test
    fun `typed validator enforces map constraints and reports map value errors on the entry`() {
        val validationResult = compiler.invokeStatic(result, "fixture.collections.Scenarios", "typedInvalid")

        assertThat(readTree(compiler.errorsJson(validationResult))).isEqualTo(
            readTree(
                """
                {
                  "labels": {"blank": {"$": [{"constraint": "NotBlank"}]}},
                  "attributes": {"$": [{"constraint": "NotEmpty"}]},
                  "limits": {"$": [{"constraint": "Size", "message": "Must have size between [0, 1]"}]},
                  "codes": {"errors": [], "items": [null, {"$": [{"constraint": "NotNull", "message": "Must not be null"}]}]}
                }
                """
            )
        )
    }

    @Test
    fun `json validator accepts valid maps and nullable list items`() {
        val validationResult = compiler.invokeStatic(
            result,
            "fixture.collections.Scenarios",
            "json",
            """
            {
              "labels": {"k": "v"},
              "attributes": {"a": "b"},
              "aliases": [null, "a"],
              "colors": {"c": "RED"},
              "codes": null
            }
            """
        )

        assertThat(compiler.errorsJson(validationResult)).isNull()
    }

    @Test
    fun `json validator type checks map values and enforces map and item constraints`() {
        val validationResult = compiler.invokeStatic(
            result,
            "fixture.collections.Scenarios",
            "json",
            """
            {
              "labels": {"ok": "x", "blank": " ", "num": 1, "nil": null},
              "attributes": {},
              "limits": {"a": 1, "b": "two"},
              "aliases": ["a", null, 3],
              "colors": {"c": "BLUE", "d": "RED"},
              "codes": ["x", null]
            }
            """
        )

        assertThat(readTree(compiler.errorsJson(validationResult))).isEqualTo(
            readTree(
                """
                {
                  "labels": {
                    "blank": {"$": [{"constraint": "NotBlank"}]},
                    "num": {"$": [{"constraint": "Type", "message": "Expected string but was number"}]},
                    "nil": {"$": [{"constraint": "NotNull", "message": "Must not be null"}]}
                  },
                  "attributes": {"$": [{"constraint": "NotEmpty", "message": "Must not be empty object"}]},
                  "limits": {
                    "b": {"$": [{"constraint": "Type", "message": "Expected integer"}]},
                    "$": [{"constraint": "Size", "message": "Must have size between [0, 1]"}]
                  },
                  "aliases": {"errors": [], "items": [null, null, {"$": [{"constraint": "Type", "message": "Expected string but was number"}]}]},
                  "colors": {"c": {"$": [{"constraint": "EnumValues", "message": "Allowed values are: [RED, GREEN]"}]}},
                  "codes": {"errors": [], "items": [null, {"$": [{"constraint": "NotNull", "message": "Must not be null"}]}]}
                }
                """
            )
        )
    }

    private fun readTree(json: String?): Any? {
        if (json == null) {
            return null
        }
        return JsonMapper.builder().build().readTree(json)
    }

}
