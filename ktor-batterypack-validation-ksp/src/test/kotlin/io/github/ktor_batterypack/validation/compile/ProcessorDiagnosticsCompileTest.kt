package io.github.ktor_batterypack.validation.compile

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.sourcesGeneratedBySymbolProcessor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Files

class ProcessorDiagnosticsCompileTest {

    companion object {
        private val compiler = ValidationProcessorCompiler()
        private val jsonMapper = JsonMapper.builder().build()

        private val levels = (1..11).joinToString("\n") { level -> "data class Level$level(val next: Level${level + 1}?)" }
        private val deepValue = (1..11).joinToString("") { level -> "Level$level(" } + "Level12(\" \")" + ")".repeat(11)

        private val validResult: JvmCompilationResult by lazy {
            val constraintDir = Files.createTempDirectory("tagged-constraints")
            Files.writeString(
                constraintDir.resolve("tagged-constraints.yaml"),
                """
                defaults:
                  className: fixture.diag.TaggedConstraints
                constraints:
                  - annotation: fixture.diag.Tagged
                    supportedTypes:
                      - kotlin.String
                """.trimIndent()
            )
            compiler.compile(listOf(VALID_MODEL), mapOf("ktor.validation.constraint.dir" to constraintDir.toString()))
        }

        private val invalidResult: JvmCompilationResult by lazy {
            compiler.compile(listOf(INVALID_MODEL))
        }

        private val VALID_MODEL = SourceFile.kotlin(
            "Account.kt",
            """
            package fixture.diag

            import io.github.ktor_batterypack.annotation.Validator
            import io.github.ktor_batterypack.validation.SingleConstraintError
            import io.github.ktor_batterypack.validation.ValidationCall
            import io.github.ktor_batterypack.validation.ValidationResult
            import jakarta.validation.constraints.Max
            import jakarta.validation.constraints.Min
            import jakarta.validation.constraints.NotBlank
            import jakarta.validation.constraints.NotEmpty
            import jakarta.validation.constraints.Size
            import java.net.URI
            import kotlin.reflect.KClass

            typealias Email = String
            typealias MaybeName = String?

            enum class Mode { STRICT, LENIENT }

            @Target(AnnotationTarget.FIELD)
            annotation class Tagged(val mode: Mode, val type: KClass<*>, val marker: Char, val weight: Float, val label: String)

            object TaggedConstraints {
                @JvmStatic
                fun checkTagged(prop: String, value: String, call: ValidationCall, mode: Mode, type: KClass<*>, marker: Char, weight: Float, label: String) {
                    if (value != label) {
                        call.propertyError(prop, SingleConstraintError("Tagged", mode.name + " " + type.simpleName + " " + marker + " " + weight + " " + label))
                    }
                }
            }

            class Account(
                @field:NotBlank
                val email: Email,
                @field:NotBlank
                val nickname: MaybeName,
                @field:Size(max = 1)
                @field:NotEmpty
                val tags: Array<String>,
                @field:Min(3000000000)
                val balance: Long,
                @field:Max(3000000000)
                val visits: Int,
                val homepage: URI?,
                @field:Tagged(mode = Mode.STRICT, type = String::class, marker = '$', weight = 1.5f, label = "cost: \${'$'}amount")
                val code: String,
                val root: Level1?,
            )

            $levels
            data class Level12(@field:NotBlank val name: String)

            @Validator
            interface AccountValidator {
                fun validateAccount(account: Account): ValidationResult<Account>

                fun defaultZone(): String = "UTC"

                fun describe(first: Account, second: Account, third: Account): String = ""

                fun isValid(account: Account): ValidationResult<Account> = validateAccount(account)
            }

            object Scenarios {
                @JvmStatic
                fun valid(): Any {
                    return AccountValidatorImpl().validateAccount(
                        Account("a@b.c", "nick", arrayOf("a"), 3_000_000_000, 5, URI("https://example.com"), "cost: \${'$'}amount", null)
                    )
                }

                @JvmStatic
                fun invalid(): Any {
                    return AccountValidatorImpl().validateAccount(
                        Account(" ", null, arrayOf("a", "b"), 2_999_999_999, 5, null, "other", $deepValue)
                    )
                }
            }
            """
        )

        private val INVALID_MODEL = SourceFile.kotlin(
            "Invalid.kt",
            """
            package fixture.diag

            import io.github.ktor_batterypack.annotation.JsonValidator
            import io.github.ktor_batterypack.annotation.Validator
            import io.github.ktor_batterypack.validation.ValidationCall
            import io.github.ktor_batterypack.validation.ValidationParamType
            import io.github.ktor_batterypack.validation.ValidationResult
            import jakarta.validation.constraints.NotBlank
            import jakarta.validation.constraints.Past
            import java.net.URI
            import java.time.Duration
            import tools.jackson.databind.JsonNode

            data class Box<T>(val value: T)

            data class Profile(
                @field:NotBlank
                val age: Int,
                @field:Past
                val cooldown: Duration,
                val extras: List<*>,
            )

            data class Link(val target: URI)

            @Validator
            interface ShapeValidator {
                fun check(first: String, second: String): Boolean

                fun checkProfile(profile: Profile?, call: ValidationCall)
            }

            @Validator
            interface BoxValidator {
                fun validateBox(box: Box<String>): ValidationResult<Box<String>>
            }

            @Validator
            interface ProfileValidator {
                fun validateProfile(profile: Profile): ValidationResult<Profile>
            }

            @JsonValidator
            interface JsonLinkValidator {
                @ValidationParamType(Link::class)
                fun validateLink(link: JsonNode): ValidationResult<JsonNode>

                fun validateUntyped(node: JsonNode): ValidationResult<JsonNode>
            }
            """
        )

        private val UNKNOWN_CONSTRAINT_MODEL = SourceFile.kotlin(
            "Counter.kt",
            """
            package fixture.unknown

            import io.github.ktor_batterypack.annotation.Validator
            import io.github.ktor_batterypack.validation.ValidationResult
            import jakarta.validation.Constraint

            @Target(AnnotationTarget.FIELD)
            @Constraint(validatedBy = [])
            annotation class Even

            data class Counter(@field:Even val value: Int)

            @Validator
            interface CounterValidator {
                fun validateCounter(counter: Counter): ValidationResult<Counter>
            }
            """
        )

        private const val UNKNOWN_CONSTRAINT_MESSAGE =
            "Constraint @Even on property `Counter.value` is not supported and is ignored"
    }

    @Test
    fun `helper functions with unsupported shapes are ignored`() {
        assertThat(validResult.exitCode).withFailMessage(validResult.messages).isEqualTo(ExitCode.OK)
        assertThat(compiler.generatedSource(validResult, "AccountValidatorImpl.kt"))
            .doesNotContain("defaultZone", "describe", "isValid")
    }

    @Test
    fun `typealiases, arrays, long bounds and deep nesting are validated`() {
        assertThat(validResult.exitCode).withFailMessage(validResult.messages).isEqualTo(ExitCode.OK)
        assertThat(compiler.errorsJson(compiler.invokeStatic(validResult, "fixture.diag.Scenarios", "valid"))).isNull()

        val errors = jsonMapper.readTree(compiler.errorsJson(compiler.invokeStatic(validResult, "fixture.diag.Scenarios", "invalid")))
        assertThat(errors.propertyNames()).containsExactlyInAnyOrder("email", "nickname", "tags", "balance", "code", "root")
        assertThat(errors.at("/nickname/0/constraint").asString()).isEqualTo("NotNull")
        assertThat(errors.at("/balance/0/message").asString()).isEqualTo("Must be at least 3000000000")
        assertThat(errors.at("/root" + "/next".repeat(11) + "/name/0/constraint").asString()).isEqualTo("NotBlank")
    }

    @Test
    fun `custom constraint arguments render as kotlin literals`() {
        assertThat(validResult.exitCode).withFailMessage(validResult.messages).isEqualTo(ExitCode.OK)
        assertThat(compiler.generatedSource(validResult, "AccountValidatorImpl.kt")).contains(
            "TaggedConstraints.checkTagged(\"code\", it, call, mode = Mode.STRICT, type = String::class, marker = '$', " +
                "weight = 1.5f, label = \"cost: \\\$amount\")"
        )

        val errors = jsonMapper.readTree(compiler.errorsJson(compiler.invokeStatic(validResult, "fixture.diag.Scenarios", "invalid")))
        assertThat(errors.at("/code/0/message").asString()).isEqualTo("STRICT String $ 1.5 cost: \$amount")
    }

    @Test
    fun `unsupported function shapes are reported`() {
        assertThat(invalidResult.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(invalidResult.messages).contains(
            "Function `check` must be declared as `fun check(value: T): ValidationResult<T>` or `fun validateT(value: T?, call: ValidationCall)`",
            "Function `checkProfile` overrides the validation of `Profile` and must be named `validateProfile`",
            "Function `validateUntyped` of a @JsonValidator must declare its model type with @ValidationParamType"
        )
    }

    @Test
    fun `unsupported property types are reported`() {
        assertThat(invalidResult.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(invalidResult.messages).contains(
            "Property `Box.value` has unsupported type `T`",
            "Property `Profile.extras` has unsupported type `List<*>`",
            "Property `Link.target` has type `java.net.URI`, which a @JsonValidator cannot read from JSON"
        )
    }

    @Test
    fun `constraints on unsupported types are reported`() {
        assertThat(invalidResult.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(invalidResult.messages).contains(
            "Constraint @NotBlank cannot be applied to property `Profile.age` of type `kotlin.Int`. Supported types: kotlin.String",
            "Constraint @Past cannot be applied to property `Profile.cooldown` of type `java.time.Duration`"
        )
    }

    @Test
    fun `validators with errors are not generated`() {
        assertThat(invalidResult.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(invalidResult.sourcesGeneratedBySymbolProcessor.map { file -> file.name }.toList()).isEmpty()
    }

    @Test
    fun `unknown constraints are reported as warnings`() {
        val result = compiler.compile(listOf(UNKNOWN_CONSTRAINT_MODEL))

        assertThat(result.exitCode).withFailMessage(result.messages).isEqualTo(ExitCode.OK)
        assertThat(result.messages.lines()).anyMatch { line -> line.startsWith("w: [ksp]") && line.contains(UNKNOWN_CONSTRAINT_MESSAGE) }
    }

    @Test
    fun `strict mode reports unknown constraints as errors`() {
        val result = compiler.compile(listOf(UNKNOWN_CONSTRAINT_MODEL), mapOf("ktor.validation.strict" to "true"))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages.lines()).anyMatch { line -> line.startsWith("e: [ksp]") && line.contains(UNKNOWN_CONSTRAINT_MESSAGE) }
    }

}
