package io.github.ktor_batterypack.validation.compile

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.sourcesGeneratedBySymbolProcessor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class MultiRoundCompileTest {

    companion object {
        private val compiler = ValidationProcessorCompiler()
        private val jsonMapper = JsonMapper.builder().build()

        private val GENERATED_ORDER = """
            package fixture.rounds

            import jakarta.validation.constraints.NotBlank
            import jakarta.validation.constraints.Size

            data class Order(
                @field:NotBlank
                val number: String,
                @field:Size(min = 1)
                val lines: List<OrderLine>,
            )
            """

        private val ORDER_VALIDATOR = SourceFile.kotlin(
            "OrderValidator.kt",
            """
            package fixture.rounds

            import io.github.ktor_batterypack.annotation.Validator
            import io.github.ktor_batterypack.validation.ValidationResult
            import jakarta.validation.constraints.Positive

            data class OrderLine(@field:Positive val quantity: Int)

            @Validator
            interface OrderValidator {
                fun validateOrder(order: Order): ValidationResult<Order>
            }

            object Scenarios {
                @JvmStatic
                fun valid(): Any {
                    return OrderValidatorImpl().validateOrder(Order("A-1", listOf(OrderLine(1))))
                }

                @JvmStatic
                fun invalid(): Any {
                    return OrderValidatorImpl().validateOrder(Order(" ", listOf(OrderLine(0))))
                }
            }
            """
        )

        private val GENERATED_INVOICE_VALIDATOR = """
            package fixture.rounds

            import io.github.ktor_batterypack.annotation.JsonValidator
            import io.github.ktor_batterypack.validation.ValidationParamType
            import io.github.ktor_batterypack.validation.ValidationResult
            import tools.jackson.databind.JsonNode

            @JsonValidator
            interface InvoiceValidator {
                @ValidationParamType(Invoice::class)
                fun validateInvoice(invoice: JsonNode): ValidationResult<JsonNode>
            }
            """

        private val INVOICE = SourceFile.kotlin(
            "Invoice.kt",
            """
            package fixture.rounds

            import jakarta.validation.constraints.NotBlank
            import tools.jackson.databind.json.JsonMapper

            data class Invoice(@field:NotBlank val number: String)

            object Scenarios {
                @JvmStatic
                fun invalid(): Any {
                    return InvoiceValidatorImpl().validateInvoice(JsonMapper.builder().build().readTree(""${'"'}{"number": " "}""${'"'}))
                }
            }
            """
        )

        private val MISSING_TYPE_MODEL = SourceFile.kotlin(
            "Shipment.kt",
            """
            package fixture.missing

            import io.github.ktor_batterypack.annotation.Validator
            import io.github.ktor_batterypack.validation.ValidationResult

            data class Shipment(val carrier: Carrier)

            @Validator
            interface ShipmentValidator {
                fun validateShipment(shipment: Shipment): ValidationResult<Shipment>
            }

            @Validator
            interface CarrierValidator {
                fun validateCarrier(carrier: Carrier): ValidationResult<Carrier>
            }
            """
        )
    }

    @Test
    fun `validators of types generated in a later round are generated`() {
        val result = compiler.compile(
            sources = listOf(ORDER_VALIDATOR),
            additionalProviders = listOf(SourceGeneratingProcessorProvider("fixture.rounds", mapOf("Order" to GENERATED_ORDER)))
        )

        assertThat(result.exitCode).withFailMessage(result.messages).isEqualTo(ExitCode.OK)
        assertThat(compiler.errorsJson(compiler.invokeStatic(result, "fixture.rounds.Scenarios", "valid"))).isNull()

        val errors = jsonMapper.readTree(compiler.errorsJson(compiler.invokeStatic(result, "fixture.rounds.Scenarios", "invalid")))
        assertThat(errors.propertyNames()).containsExactlyInAnyOrder("number", "lines")
        assertThat(errors.at("/lines/items/0/quantity/0/constraint").asString()).isEqualTo("Positive")
    }

    @Test
    fun `validators declared in a later round are generated`() {
        val result = compiler.compile(
            sources = listOf(INVOICE),
            additionalProviders = listOf(
                SourceGeneratingProcessorProvider("fixture.rounds", mapOf("InvoiceValidator" to GENERATED_INVOICE_VALIDATOR))
            )
        )

        assertThat(result.exitCode).withFailMessage(result.messages).isEqualTo(ExitCode.OK)
        val errors = jsonMapper.readTree(compiler.errorsJson(compiler.invokeStatic(result, "fixture.rounds.Scenarios", "invalid")))
        assertThat(errors.at("/number/0/constraint").asString()).isEqualTo("NotBlank")
    }

    @Test
    fun `types that stay unresolved are reported when processing ends`() {
        val result = compiler.compile(listOf(MISSING_TYPE_MODEL))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(
            "Property `Shipment.carrier` has unresolved type",
            "Could not generate a validator for fixture.missing.CarrierValidator: some of its types could not be resolved"
        )
        assertThat(result.messages).doesNotContain("unsupported type")
        assertThat(result.sourcesGeneratedBySymbolProcessor.map { file -> file.name }.toList()).isEmpty()
    }

}
