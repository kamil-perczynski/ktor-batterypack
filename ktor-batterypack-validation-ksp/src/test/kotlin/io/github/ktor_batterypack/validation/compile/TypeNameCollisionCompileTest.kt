package io.github.ktor_batterypack.validation.compile

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

class TypeNameCollisionCompileTest {

    companion object {
        private val compiler = ValidationProcessorCompiler()
        private val jsonMapper = JsonMapper.builder().build()

        private val result: JvmCompilationResult by lazy {
            compiler.compile(listOf(ADDRESS_A, ADDRESS_B, MODEL, SCENARIOS))
        }

        private val ADDRESS_A = SourceFile.kotlin(
            "AddressA.kt",
            """
            package fixture.collision.a

            import jakarta.validation.constraints.NotBlank

            data class Address(@field:NotBlank val street: String, val kind: Kind)

            enum class Kind { HOME, OFFICE }
            """
        )

        private val ADDRESS_B = SourceFile.kotlin(
            "AddressB.kt",
            """
            package fixture.collision.b

            import jakarta.validation.constraints.NotBlank
            import jakarta.validation.constraints.Size

            data class Address(
                @field:NotBlank val city: String,
                @field:Size(max = 1) val lines: List<String>,
                val kind: Kind,
            )

            enum class Kind { DEPOT }
            """
        )

        private val MODEL = SourceFile.kotlin(
            "Shipment.kt",
            """
            package fixture.collision

            import io.github.ktor_batterypack.annotation.JsonValidator
            import io.github.ktor_batterypack.annotation.Validator
            import io.github.ktor_batterypack.validation.SingleConstraintError
            import io.github.ktor_batterypack.validation.ValidationCall
            import io.github.ktor_batterypack.validation.ValidationParamType
            import io.github.ktor_batterypack.validation.ValidationResult
            import tools.jackson.databind.JsonNode

            data class Shipment(
                val from: fixture.collision.a.Address,
                val to: fixture.collision.b.Address,
                val legs: List<List<fixture.collision.a.Address>>,
                val depots: List<List<fixture.collision.b.Address>>,
            )

            @Validator
            interface ShipmentValidator {
                fun validateShipment(shipment: Shipment): ValidationResult<Shipment>

                fun validateBAddress(address: fixture.collision.b.Address?, call: ValidationCall) {
                    if (address?.city == "Nowhere") {
                        call.propertyError("city", SingleConstraintError("KnownCity"))
                    }
                }
            }

            @JsonValidator
            interface JsonShipmentValidator {
                @ValidationParamType(Shipment::class)
                fun validateShipment(shipment: JsonNode): ValidationResult<JsonNode>
            }
            """
        )

        private val SCENARIOS = SourceFile.kotlin(
            "Scenarios.kt",
            """
            package fixture.collision

            import fixture.collision.a.Kind
            import tools.jackson.databind.json.JsonMapper

            object Scenarios {

                @JvmStatic
                fun typed(street: String, city: String): Any {
                    val from = fixture.collision.a.Address(street, Kind.HOME)
                    val to = fixture.collision.b.Address(city, listOf("1", "2"), fixture.collision.b.Kind.DEPOT)
                    return ShipmentValidatorImpl().validateShipment(Shipment(from, to, listOf(listOf(from)), listOf(listOf(to))))
                }

                @JvmStatic
                fun json(json: String): Any {
                    return JsonShipmentValidatorImpl().validateShipment(JsonMapper.builder().build().readTree(json))
                }
            }
            """
        )

        private val MISNAMED_OVERRIDE = SourceFile.kotlin(
            "Misnamed.kt",
            """
            package fixture.collision

            import io.github.ktor_batterypack.annotation.Validator
            import io.github.ktor_batterypack.validation.ValidationCall
            import io.github.ktor_batterypack.validation.ValidationResult

            data class Route(val from: fixture.collision.a.Address, val to: fixture.collision.b.Address)

            @Validator
            interface RouteValidator {
                fun validateRoute(route: Route): ValidationResult<Route>

                fun validateAddress(address: fixture.collision.b.Address?, call: ValidationCall)
            }
            """
        )
    }

    @Test
    fun `types sharing a simple name generate separate validation methods`() {
        assertThat(result.exitCode).withFailMessage(result.messages).isEqualTo(ExitCode.OK)

        val source = compiler.generatedSource(result, "ShipmentValidatorImpl.kt")
        assertThat(source).contains(
            "private fun validateAAddress(address: fixture.collision.a.Address?, call: ValidationCall)",
            "private fun validateListAAddress(list: List<fixture.collision.a.Address>?, call: ValidationCall)",
            "private fun validateListBAddress(list: List<fixture.collision.b.Address>?, call: ValidationCall)",
            "private fun validateAKind(kind: fixture.collision.a.Kind?, call: ValidationCall)",
        )
        assertThat(source).doesNotContain("import fixture.collision.a.", "import fixture.collision.b.")
        assertThat(compiler.generatedSource(result, "JsonShipmentValidatorImpl.kt")).contains(
            "private fun validateAAddress(address: ObjectNode?, call: ValidationCall)",
            "private fun validateBAddress(address: ObjectNode?, call: ValidationCall)",
            "private fun validateListAAddress(list: ArrayNode?, call: ValidationCall)",
            "private fun validateListBAddress(list: ArrayNode?, call: ValidationCall)",
            "private fun validateAKind(kind: JsonNode?, call: ValidationCall, prop: String)",
            "private fun validateBKind(kind: JsonNode?, call: ValidationCall, prop: String)",
        )
    }

    @Test
    fun `typed validator validates both colliding types`() {
        assertThat(compiler.errorsJson(compiler.invokeStatic(result, "fixture.collision.Scenarios", "typed", "Main", "Berlin")))
            .isNull()

        val errors = readTree(compiler.errorsJson(compiler.invokeStatic(result, "fixture.collision.Scenarios", "typed", " ", "Nowhere")))

        assertThat(errors.at("/from/street/0/constraint").asString()).isEqualTo("NotBlank")
        assertThat(errors.at("/legs/items/0/items/0/street/0/constraint").asString()).isEqualTo("NotBlank")
        assertThat(errors.at("/to/city/0/constraint").asString()).isEqualTo("KnownCity")
        assertThat(errors.at("/depots/items/0/items/0/city/0/constraint").asString()).isEqualTo("KnownCity")
        assertThat(errors.at("/to/lines").isMissingNode).isTrue()
    }

    @Test
    fun `json validator validates both colliding types`() {
        val errors = readTree(
            compiler.errorsJson(
                compiler.invokeStatic(
                    result,
                    "fixture.collision.Scenarios",
                    "json",
                    """
                    {
                      "from": {"street": " ", "kind": "DEPOT"},
                      "to": {"city": " ", "lines": ["1", "2"], "kind": "HOME"},
                      "legs": [[{"street": " ", "kind": "HOME"}]],
                      "depots": [[{"city": "Berlin", "lines": [], "kind": "DEPOT"}]]
                    }
                    """
                )
            )
        )

        assertThat(errors.at("/from/street/0/constraint").asString()).isEqualTo("NotBlank")
        assertThat(errors.at("/from/kind/0/constraint").asString()).isEqualTo("EnumValues")
        assertThat(errors.at("/to/city/0/constraint").asString()).isEqualTo("NotBlank")
        assertThat(errors.at("/to/lines/errors/0/constraint").asString()).isEqualTo("Size")
        assertThat(errors.at("/to/kind/0/constraint").asString()).isEqualTo("EnumValues")
        assertThat(errors.at("/legs/items/0/items/0/street/0/constraint").asString()).isEqualTo("NotBlank")
        assertThat(errors.at("/depots").isMissingNode).isTrue()
    }

    @Test
    fun `overrides of colliding types must use the qualified method name`() {
        val misnamed = compiler.compile(listOf(ADDRESS_A, ADDRESS_B, MISNAMED_OVERRIDE))

        assertThat(misnamed.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(misnamed.messages).contains(
            "Function `validateAddress` overrides the validation of `Address` and must be named `validateBAddress`"
        )
    }

    private fun readTree(json: String?): JsonNode {
        return jsonMapper.readTree(json)
    }
}
