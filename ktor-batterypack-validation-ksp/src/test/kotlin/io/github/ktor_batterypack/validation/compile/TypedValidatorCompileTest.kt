package io.github.ktor_batterypack.validation.compile

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class TypedValidatorCompileTest {

    companion object {
        private val compiler = ValidationProcessorCompiler()

        private val result: JvmCompilationResult by lazy {
            compiler.compile(listOf(MODEL, VALIDATOR, SCENARIOS))
        }

        private val MODEL = SourceFile.kotlin(
            "Order.kt",
            """
            package fixture.typed

            import jakarta.validation.constraints.DecimalMin
            import jakarta.validation.constraints.Max
            import jakarta.validation.constraints.Min
            import jakarta.validation.constraints.NotBlank
            import jakarta.validation.constraints.NotEmpty
            import jakarta.validation.constraints.Past
            import jakarta.validation.constraints.Pattern
            import jakarta.validation.constraints.Positive
            import jakarta.validation.constraints.PositiveOrZero
            import jakarta.validation.constraints.Size
            import java.math.BigDecimal
            import java.time.LocalDate

            enum class Status { ACTIVE, BLOCKED }

            data class Address(
                @field:NotBlank
                val street: String,
                @field:Pattern(regexp = "\\d{2}-\\d{3}")
                val zipCode: String?,
            )

            data class Order(
                @field:NotBlank
                @field:Size(min = 2, max = 10)
                val name: String,
                @field:Min(1)
                @field:Max(10)
                val quantity: Int,
                @field:Positive
                val total: Long,
                @field:PositiveOrZero
                val ratio: Double,
                @field:DecimalMin("0.01")
                val price: BigDecimal,
                val active: Boolean,
                @field:Past
                val createdOn: LocalDate,
                val status: Status,
                val address: Address,
                val shipping: Address?,
                @field:NotEmpty
                val lines: List<Address>,
                @field:Size(min = 1, max = 3)
                val tags: List<String>,
            )
            """
        )

        private val VALIDATOR = SourceFile.kotlin(
            "OrderValidator.kt",
            """
            package fixture.typed

            import io.github.ktor_batterypack.annotation.Validator
            import io.github.ktor_batterypack.validation.ValidationResult

            @Validator
            interface OrderValidator {
                fun validateOrder(order: Order): ValidationResult<Order>
            }
            """
        )

        private val SCENARIOS = SourceFile.kotlin(
            "Scenarios.kt",
            """
            package fixture.typed

            import java.math.BigDecimal
            import java.time.LocalDate

            object Scenarios {

                @JvmStatic
                fun valid(): Any {
                    return OrderValidatorImpl().validateOrder(
                        Order(
                            name = "order",
                            quantity = 10,
                            total = 1,
                            ratio = 0.0,
                            price = BigDecimal("0.01"),
                            active = true,
                            createdOn = LocalDate.of(2020, 1, 1),
                            status = Status.ACTIVE,
                            address = Address("Main", "12-345"),
                            shipping = null,
                            lines = listOf(Address("Side", null)),
                            tags = listOf("a"),
                        )
                    )
                }

                @JvmStatic
                fun invalid(): Any {
                    return OrderValidatorImpl().validateOrder(
                        Order(
                            name = " ",
                            quantity = 0,
                            total = 0,
                            ratio = -1.0,
                            price = BigDecimal("0.009"),
                            active = false,
                            createdOn = LocalDate.now().plusDays(1),
                            status = Status.BLOCKED,
                            address = Address("", "12345"),
                            shipping = Address(" ", null),
                            lines = emptyList(),
                            tags = listOf("a", "b", "c", "d"),
                        )
                    )
                }
            }
            """
        )
    }

    @Test
    fun `typed validator for scalar, enum, nested and list properties compiles`() {
        assertThat(result.exitCode).withFailMessage(result.messages).isEqualTo(ExitCode.OK)
        assertThat(compiler.generatedSource(result, "OrderValidatorImpl.kt")).contains("class OrderValidatorImpl")
    }

    @Test
    fun `typed validator accepts a valid object`() {
        val validationResult = compiler.invokeStatic(result, "fixture.typed.Scenarios", "valid")

        assertThat(compiler.errorsJson(validationResult)).isNull()
    }

    @Test
    fun `typed validator reports every violated constraint`() {
        val validationResult = compiler.invokeStatic(result, "fixture.typed.Scenarios", "invalid")

        assertThat(readTree(compiler.errorsJson(validationResult))).isEqualTo(
            readTree(
                """
                {
                  "name": [
                    {"constraint": "NotBlank"},
                    {"constraint": "Size", "message": "Must be longer than min=2"}
                  ],
                  "quantity": [{"constraint": "Min", "message": "Must be at least 1"}],
                  "total": [{"constraint": "Positive"}],
                  "ratio": [{"constraint": "PositiveOrZero"}],
                  "price": [{"constraint": "DecimalMin", "message": "Must be greater than or equal to 0.01"}],
                  "createdOn": [{"constraint": "Past"}],
                  "address": {
                    "street": [{"constraint": "NotBlank"}],
                    "zipCode": [{"constraint": "Pattern", "message": "Must match \\d{2}-\\d{3}"}]
                  },
                  "shipping": {
                    "street": [{"constraint": "NotBlank"}]
                  },
                  "lines": {"errors": [{"constraint": "NotEmpty"}], "items": []},
                  "tags": {"errors": [{"constraint": "Size", "message": "Must have size between [1, 3]"}], "items": [null, null, null, null]}
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
