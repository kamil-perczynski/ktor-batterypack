package io.github.ktor_batterypack.validation.compile

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class InjectionAndSuspendCompileTest {

    companion object {
        private val compiler = ValidationProcessorCompiler()
        private val jsonMapper = JsonMapper.builder().build()

        private val result: JvmCompilationResult by lazy {
            compiler.compile(listOf(MODEL, VALIDATORS, SCENARIOS))
        }

        private val MODEL = SourceFile.kotlin(
            "Account.kt",
            """
            package fixture.inject

            import jakarta.validation.constraints.NotBlank
            import jakarta.validation.constraints.Past
            import java.time.Instant

            data class Account(
                @field:NotBlank val email: String,
                @field:Past val createdAt: Instant,
                val address: Address,
            )

            data class Address(@field:NotBlank val city: String, val zip: String)

            interface EmailRegistry {
                fun isTaken(email: String): Boolean
            }

            interface ZipCodes {
                suspend fun isKnown(zip: String): Boolean
            }
            """
        )

        private val VALIDATORS = SourceFile.kotlin(
            "AccountValidators.kt",
            """
            package fixture.inject

            import io.github.ktor_batterypack.annotation.Validator
            import io.github.ktor_batterypack.validation.SingleConstraintError
            import io.github.ktor_batterypack.validation.ValidationCall
            import io.github.ktor_batterypack.validation.ValidationResult
            import java.time.Clock

            @Validator
            interface AccountValidator {
                val emailRegistry: EmailRegistry
                val zipCodes: ZipCodes
                val clock: Clock

                suspend fun validateAccount(account: Account): ValidationResult<Account>

                fun checkEmail(account: Account?, call: ValidationCall) {
                    if (account == null) return
                    if (emailRegistry.isTaken(account.email)) {
                        call.propertyError("email", SingleConstraintError("EmailTaken", "Email is taken"))
                    }
                }

                suspend fun checkZip(address: Address?, call: ValidationCall) {
                    if (address == null) return
                    if (!zipCodes.isKnown(address.zip)) {
                        call.propertyError("zip", SingleConstraintError("ZipUnknown", "Unknown zip code"))
                    }
                }
            }

            @Validator
            interface AddressValidator {
                fun validateAddress(address: Address): ValidationResult<Address>
            }
            """
        )

        private val SCENARIOS = SourceFile.kotlin(
            "Scenarios.kt",
            """
            package fixture.inject

            import java.time.Clock
            import java.time.Instant
            import java.time.ZoneOffset
            import kotlin.coroutines.Continuation
            import kotlin.coroutines.EmptyCoroutineContext
            import kotlin.coroutines.resume
            import kotlin.coroutines.startCoroutine
            import kotlin.coroutines.suspendCoroutine

            object Scenarios {

                private val registry = object : EmailRegistry {
                    override fun isTaken(email: String): Boolean = email == "taken@example.com"
                }

                private val zipCodes = object : ZipCodes {
                    override suspend fun isKnown(zip: String): Boolean {
                        return suspendCoroutine { continuation -> continuation.resume(zip.startsWith("0")) }
                    }
                }

                private val validator = AccountValidatorImpl(
                    emailRegistry = registry,
                    zipCodes = zipCodes,
                    clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
                )

                @JvmStatic
                fun validate(email: String, createdAt: String, zip: String): Any {
                    val account = Account(email, Instant.parse(createdAt), Address("Warsaw", zip))
                    var outcome: Result<Any>? = null
                    val block: suspend () -> Any = { validator.validateAccount(account) }
                    block.startCoroutine(Continuation(EmptyCoroutineContext) { value -> outcome = value })
                    return outcome!!.getOrThrow()
                }
            }
            """
        )
    }

    @Test
    fun `validators with injected properties and suspend checks compile`() {
        assertThat(result.exitCode).withFailMessage(result.messages).isEqualTo(ExitCode.OK)
    }

    @Test
    fun `abstract properties become constructor properties`() {
        assertThat(compiler.generatedSource(result, "AccountValidatorImpl.kt")).contains(
            """
                |public class AccountValidatorImpl(
                |  override val emailRegistry: EmailRegistry,
                |  override val zipCodes: ZipCodes,
                |  override val clock: Clock = Clock.systemDefaultZone(),
                |) : AccountValidator {
            """.trimMargin().replace("  ", "    ")
        )
    }

    @Test
    fun `methods that reach a suspend check are suspend`() {
        val source = compiler.generatedSource(result, "AccountValidatorImpl.kt")

        assertThat(source)
            .contains("override suspend fun validateAccount(account: Account): ValidationResult<Account>")
            .contains("private suspend fun validateAccount(account: Account?, call: ValidationCall)")
            .contains("private suspend fun validateAddress(address: Address?, call: ValidationCall)")
    }

    @Test
    fun `suspend validator accepts a valid account`() {
        val validationResult = compiler.invokeStatic(
            result, "fixture.inject.Scenarios", "validate", "new@example.com", "2025-01-01T00:00:00Z", "00-950"
        )

        assertThat(compiler.errorsJson(validationResult)).isNull()
    }

    @Test
    fun `suspend validator reports injected and suspend checks`() {
        val validationResult = compiler.invokeStatic(
            result, "fixture.inject.Scenarios", "validate", "taken@example.com", "2027-01-01T00:00:00Z", "99-999"
        )

        assertThat(readTree(compiler.errorsJson(validationResult))).isEqualTo(
            readTree(
                """
                {
                  "email": [{"constraint": "EmailTaken", "message": "Email is taken"}],
                  "createdAt": [{"constraint": "Past"}],
                  "address": {"zip": [{"constraint": "ZipUnknown", "message": "Unknown zip code"}]}
                }
                """
            )
        )
    }

    @Test
    fun `validator without suspend functions or properties keeps its shape`() {
        assertThat(compiler.generatedSource(result, "AddressValidatorImpl.kt")).isEqualTo(
            """
            |package fixture.inject
            |
            |import io.github.ktor_batterypack.validation.Constraints
            |import io.github.ktor_batterypack.validation.ValidationCall
            |import io.github.ktor_batterypack.validation.ValidationResult
            |import java.time.Clock
            |import javax.`annotation`.processing.Generated
            |import kotlin.Suppress
            |
            |/**
            | * Validator implementation for [AddressValidator].
            | *
            | * NOTE: This class is auto generated by Ktor-Batterypack-Validation (https://github.com/kamil-perczynski/ktor-batterypack).
            | * Do not edit the class manually.
            | */
            |@Generated
            |@Suppress("UNNECESSARY_SAFE_CALL")
            |public class AddressValidatorImpl(
            |    private val clock: Clock = Clock.systemDefaultZone(),
            |) : AddressValidator {
            |    override fun validateAddress(address: Address): ValidationResult<Address> {
            |        val call = ValidationCall(clock = clock)
            |        validateAddress(address, call)
            |        val errors = call.finishObject()
            |        return if (errors != null) ValidationResult.invalid(address, errors) else ValidationResult.valid(address)
            |    }
            |
            |    private fun validateAddress(address: Address?, call: ValidationCall) {
            |        if (address == null) return
            |        address.city?.let {
            |            Constraints.checkNotBlank("city", it, call)
            |        }
            |    }
            |}
            |
            """.trimMargin()
        )
    }

    @Test
    fun `invalid properties and missing suspend modifiers fail the build`() {
        val failed = compiler.compile(
            listOf(
                SourceFile.kotlin(
                    "Broken.kt",
                    """
                    package fixture.inject.broken

                    import io.github.ktor_batterypack.annotation.Validator
                    import io.github.ktor_batterypack.validation.ValidationCall
                    import io.github.ktor_batterypack.validation.ValidationResult

                    data class Order(val line: Line)
                    data class Line(val sku: String)

                    @Validator
                    interface OrderValidator {
                        var registry: String
                        val clock: String

                        fun validate(order: Order): ValidationResult<Order>

                        suspend fun checkLine(line: Line?, call: ValidationCall) {}
                    }

                    data class Shipment(val order: Order)

                    @Validator
                    interface ShipmentValidator {
                        suspend fun validate(shipment: Shipment): ValidationResult<Shipment>

                        fun validateOrder(order: Order?, call: ValidationCall)

                        suspend fun checkLine(line: Line?, call: ValidationCall) {}
                    }
                    """
                )
            )
        )

        assertThat(failed.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(failed.messages)
            .contains("Property `registry` of `OrderValidator` must be a `val`")
            .contains("Property `clock` of `OrderValidator` must have type `java.time.Clock`")
            .contains("Function `validate` must be declared `suspend` because it calls suspend function `checkLine`")
            .contains("Function `validateOrder` must be declared `suspend` because it calls suspend function `checkLine`")
    }

    private fun readTree(json: String?): Any? {
        if (json == null) {
            return null
        }
        return jsonMapper.readTree(json)
    }

}
