package io.github.ktor_batterypack.validation.compile

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Files
import java.nio.file.Path

class RepeatedConstraintsCompileTest {

    companion object {
        private val compiler = ValidationProcessorCompiler()
        private val jsonMapper = JsonMapper.builder().build()

        private val result: JvmCompilationResult by lazy {
            compiler.compile(listOf(MODEL, SCENARIOS))
        }

        private val MODEL = SourceFile.kotlin(
            "Coupon.kt",
            """
            package fixture.repeated

            import io.github.ktor_batterypack.annotation.JsonValidator
            import io.github.ktor_batterypack.annotation.Validator
            import io.github.ktor_batterypack.validation.ValidationParamType
            import io.github.ktor_batterypack.validation.ValidationResult
            import jakarta.validation.constraints.Pattern
            import tools.jackson.databind.JsonNode

            data class Coupon(
                @field:Pattern(regexp = "^A.*")
                @field:Pattern(regexp = ".*Z$")
                val code: String,
                @field:Pattern.List(Pattern(regexp = "^[0-9]+$"), Pattern(regexp = "^.{4}$"))
                val pin: String,
            )

            @Validator
            interface CouponValidator {
                fun validateCoupon(coupon: Coupon): ValidationResult<Coupon>
            }

            @JsonValidator
            interface JsonCouponValidator {
                @ValidationParamType(Coupon::class)
                fun validateCoupon(coupon: JsonNode): ValidationResult<JsonNode>
            }
            """
        )

        private val SCENARIOS = SourceFile.kotlin(
            "Scenarios.kt",
            """
            package fixture.repeated

            import tools.jackson.databind.json.JsonMapper

            object Scenarios {

                @JvmStatic
                fun typed(code: String, pin: String): Any {
                    return CouponValidatorImpl().validateCoupon(Coupon(code, pin))
                }

                @JvmStatic
                fun json(json: String): Any {
                    return JsonCouponValidatorImpl().validateCoupon(JsonMapper.builder().build().readTree(json))
                }
            }
            """
        )
    }

    @Test
    fun `repeated and contained constraints are all generated`() {
        assertThat(result.exitCode).withFailMessage(result.messages).isEqualTo(ExitCode.OK)

        val source = compiler.generatedSource(result, "CouponValidatorImpl.kt")
        assertThat(source).contains("\"^A.*\"", "\".*Z$\"", "\"^[0-9]+$\"", "\"^.{4}$\"")
    }

    @Test
    fun `typed validator enforces every repeated constraint`() {
        assertThat(compiler.errorsJson(compiler.invokeStatic(result, "fixture.repeated.Scenarios", "typed", "AbcZ", "1234")))
            .isNull()

        val startOnly = readTree(compiler.errorsJson(compiler.invokeStatic(result, "fixture.repeated.Scenarios", "typed", "Abc", "12a")))
        assertThat(startOnly.at("/code").size()).isEqualTo(1)
        assertThat(startOnly.at("/pin").size()).isEqualTo(2)

        val neither = readTree(compiler.errorsJson(compiler.invokeStatic(result, "fixture.repeated.Scenarios", "typed", "bcd", "12345")))
        assertThat(neither.at("/code").size()).isEqualTo(2)
        assertThat(neither.at("/pin").size()).isEqualTo(1)
    }

    @Test
    fun `json validator enforces every repeated constraint`() {
        val errors = readTree(
            compiler.errorsJson(
                compiler.invokeStatic(result, "fixture.repeated.Scenarios", "json", """{"code": "bcd", "pin": "12a"}""")
            )
        )

        assertThat(errors.at("/code").size()).isEqualTo(2)
        assertThat(errors.at("/pin").size()).isEqualTo(2)
        assertThat(errors.at("/code/0/constraint").asString()).isEqualTo("Pattern")
    }

    @Test
    fun `custom constraints sharing a simple name with a jakarta constraint are both enforced`(@TempDir constraintDir: Path) {
        Files.writeString(
            constraintDir.resolve("email-constraints.yaml"),
            """
            defaults:
              className: fixture.email.EmailRules
            constraints:
              - annotation: fixture.email.Email
                methodName: checkCorporateEmail
            """.trimIndent()
        )

        val custom = compiler.compile(
            listOf(
                SourceFile.kotlin(
                    "Contact.kt",
                    """
                    package fixture.email

                    import io.github.ktor_batterypack.annotation.Validator
                    import io.github.ktor_batterypack.validation.SingleConstraintError
                    import io.github.ktor_batterypack.validation.ValidationCall
                    import io.github.ktor_batterypack.validation.ValidationResult

                    annotation class Email

                    object EmailRules {
                        @JvmStatic
                        fun checkCorporateEmail(prop: String, value: String, call: ValidationCall) {
                            if (!value.endsWith("@corp.example")) {
                                call.propertyError(prop, SingleConstraintError("CorporateEmail"))
                            }
                        }
                    }

                    data class Contact(
                        @field:jakarta.validation.constraints.Email
                        @field:Email
                        val email: String,
                    )

                    @Validator
                    interface ContactValidator {
                        fun validateContact(contact: Contact): ValidationResult<Contact>
                    }

                    object Scenarios {
                        @JvmStatic
                        fun validate(email: String): Any {
                            return ContactValidatorImpl().validateContact(Contact(email))
                        }
                    }
                    """
                )
            ),
            processorOptions = mapOf("ktor.validation.constraint.dir" to constraintDir.toString())
        )

        assertThat(custom.exitCode).withFailMessage(custom.messages).isEqualTo(ExitCode.OK)
        assertThat(compiler.generatedSource(custom, "ContactValidatorImpl.kt"))
            .contains("Constraints.checkEmail(", "EmailRules.checkCorporateEmail(")
        assertThat(compiler.errorsJson(compiler.invokeStatic(custom, "fixture.email.Scenarios", "validate", "jane@corp.example")))
            .isNull()

        val errors = readTree(compiler.errorsJson(compiler.invokeStatic(custom, "fixture.email.Scenarios", "validate", "jane")))
        assertThat(errors.at("/email").size()).isEqualTo(2)
        assertThat(errors.at("/email/0/constraint").asString()).isEqualTo("Email")
        assertThat(errors.at("/email/1/constraint").asString()).isEqualTo("CorporateEmail")
    }

    private fun readTree(json: String?): JsonNode {
        return jsonMapper.readTree(json)
    }
}
