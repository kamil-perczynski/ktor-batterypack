package io.github.ktor_batterypack.validation.compile

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class JsonPropertyModelCompileTest {

    companion object {
        private val compiler = ValidationProcessorCompiler()

        private val result: JvmCompilationResult by lazy {
            compiler.compile(listOf(MODEL, VALIDATORS, SCENARIOS))
        }

        private val MODEL = SourceFile.kotlin(
            "Account.kt",
            """
            package fixture.model

            import com.fasterxml.jackson.annotation.JsonIgnore
            import com.fasterxml.jackson.annotation.JsonProperty
            import jakarta.validation.constraints.NotBlank
            import jakarta.validation.constraints.Size

            data class Account(
                @JsonProperty("first_name")
                @field:NotBlank
                val firstName: String,
                val age: Int,
                @JsonIgnore
                val secret: String = "",
                val nickname: String = "anon",
                val tags: List<String> = emptyList(),
                @param:Size(max = 3)
                val paramCode: String? = null,
                @get:Size(max = 3)
                val getterCode: String? = null,
                @field:Size(max = 3)
                val fieldCode: String? = null,
                @Size(max = 3)
                val bareCode: String? = null,
            ) {
                val isAdult: Boolean
                    get() = age >= 18
            }

            abstract class Audited(
                @field:NotBlank
                val createdBy: String
            )

            class Note(createdBy: String, val text: String) : Audited(createdBy)
            """
        )

        private val VALIDATORS = SourceFile.kotlin(
            "AccountValidators.kt",
            """
            package fixture.model

            import io.github.ktor_batterypack.annotation.JsonValidator
            import io.github.ktor_batterypack.annotation.Validator
            import io.github.ktor_batterypack.validation.ValidationParamType
            import io.github.ktor_batterypack.validation.ValidationResult
            import tools.jackson.databind.JsonNode

            @JsonValidator
            interface AccountValidator {
                @ValidationParamType(Account::class)
                fun validateAccount(account: JsonNode): ValidationResult<JsonNode>

                @ValidationParamType(Note::class)
                fun validateNote(note: JsonNode): ValidationResult<JsonNode>
            }

            @Validator
            interface TypedAccountValidator {
                fun validateAccount(account: Account): ValidationResult<Account>
            }
            """
        )

        private val SCENARIOS = SourceFile.kotlin(
            "Scenarios.kt",
            """
            package fixture.model

            import tools.jackson.databind.json.JsonMapper

            object Scenarios {

                @JvmStatic
                fun validateAccount(json: String): Any {
                    return AccountValidatorImpl().validateAccount(JsonMapper.builder().build().readTree(json))
                }

                @JvmStatic
                fun validateNote(json: String): Any {
                    return AccountValidatorImpl().validateNote(JsonMapper.builder().build().readTree(json))
                }

                @JvmStatic
                fun validateTypedAccount(): Any {
                    return TypedAccountValidatorImpl().validateAccount(
                        Account(
                            firstName = " ",
                            age = 30,
                            paramCode = "abcd",
                            getterCode = "abcd",
                            fieldCode = "abcd",
                            bareCode = "abcd",
                        )
                    )
                }
            }
            """
        )
    }

    @Test
    fun `validators for json property model compile`() {
        assertThat(result.exitCode).withFailMessage(result.messages).isEqualTo(ExitCode.OK)
    }

    @Test
    fun `json validator reads the json property name and does not require computed or defaulted properties`() {
        val validationResult = compiler.invokeStatic(
            result,
            "fixture.model.Scenarios",
            "validateAccount",
            """{"first_name": "Ann", "age": 20}"""
        )

        assertThat(compiler.errorsJson(validationResult)).isNull()
    }

    @Test
    fun `json validator reports missing properties under their json name`() {
        val validationResult = compiler.invokeStatic(
            result,
            "fixture.model.Scenarios",
            "validateAccount",
            """{"firstName": "Ann"}"""
        )

        assertThat(readTree(compiler.errorsJson(validationResult))).isEqualTo(
            readTree(
                """
                {
                  "first_name": [{"constraint": "NotNull", "message": "Must not be null"}],
                  "age": [{"constraint": "NotNull", "message": "Must not be null"}]
                }
                """
            )
        )
    }

    @Test
    fun `json validator skips ignored properties and applies annotations from every use-site target`() {
        val validationResult = compiler.invokeStatic(
            result,
            "fixture.model.Scenarios",
            "validateAccount",
            """
            {
              "first_name": " ",
              "age": 20,
              "secret": 42,
              "paramCode": "abcd",
              "getterCode": "abcd",
              "fieldCode": "abcd",
              "bareCode": "abcd"
            }
            """
        )

        assertThat(readTree(compiler.errorsJson(validationResult))).isEqualTo(
            readTree(
                """
                {
                  "first_name": [{"constraint": "NotBlank"}],
                  "paramCode": [{"constraint": "Size", "message": "Must be shorter than max=3"}],
                  "getterCode": [{"constraint": "Size", "message": "Must be shorter than max=3"}],
                  "fieldCode": [{"constraint": "Size", "message": "Must be shorter than max=3"}],
                  "bareCode": [{"constraint": "Size", "message": "Must be shorter than max=3"}]
                }
                """
            )
        )
    }

    @Test
    fun `json validator includes inherited properties`() {
        val missing = compiler.invokeStatic(result, "fixture.model.Scenarios", "validateNote", """{"text": "x"}""")
        val blank = compiler.invokeStatic(
            result,
            "fixture.model.Scenarios",
            "validateNote",
            """{"createdBy": " ", "text": "x"}"""
        )

        assertThat(readTree(compiler.errorsJson(missing))).isEqualTo(
            readTree("""{"createdBy": [{"constraint": "NotNull", "message": "Must not be null"}]}""")
        )
        assertThat(readTree(compiler.errorsJson(blank))).isEqualTo(
            readTree("""{"createdBy": [{"constraint": "NotBlank"}]}""")
        )
    }

    @Test
    fun `typed validator keeps kotlin names and applies annotations from every use-site target`() {
        val validationResult = compiler.invokeStatic(result, "fixture.model.Scenarios", "validateTypedAccount")

        assertThat(readTree(compiler.errorsJson(validationResult))).isEqualTo(
            readTree(
                """
                {
                  "firstName": [{"constraint": "NotBlank"}],
                  "paramCode": [{"constraint": "Size", "message": "Must be shorter than max=3"}],
                  "getterCode": [{"constraint": "Size", "message": "Must be shorter than max=3"}],
                  "fieldCode": [{"constraint": "Size", "message": "Must be shorter than max=3"}],
                  "bareCode": [{"constraint": "Size", "message": "Must be shorter than max=3"}]
                }
                """
            )
        )
    }

    @Test
    fun `computed properties are not part of the generated validator`() {
        assertThat(compiler.generatedSource(result, "AccountValidatorImpl.kt"))
            .doesNotContain("isAdult")
            .doesNotContain("\"secret\"")
        assertThat(compiler.generatedSource(result, "TypedAccountValidatorImpl.kt"))
            .doesNotContain("isAdult")
    }

    private fun readTree(json: String?): Any? {
        if (json == null) {
            return null
        }
        return JsonMapper.builder().build().readTree(json)
    }

}
