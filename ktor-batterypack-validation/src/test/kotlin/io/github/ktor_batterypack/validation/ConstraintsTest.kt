package io.github.ktor_batterypack.validation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

class ConstraintsTest {

    @Test
    fun `inclusive DecimalMin accepts the bound`() {
        val call = ValidationCall()

        Constraints.checkDecimalMin("amount", BigDecimal("0.01"), call, value = "0.01", inclusive = true)

        assertThat(call.finishObject()).isNull()
    }

    @Test
    fun `inclusive DecimalMin rejects values below the bound`() {
        val call = ValidationCall()

        Constraints.checkDecimalMin("amount", BigDecimal("0.009"), call, value = "0.01", inclusive = true)

        assertThat(errorsOf(call, "amount")).containsExactly(
            SingleConstraintError("DecimalMin", "Must be greater than or equal to 0.01")
        )
    }

    @Test
    fun `exclusive DecimalMin rejects the bound`() {
        val call = ValidationCall()

        Constraints.checkDecimalMin("amount", BigDecimal.ZERO, call, value = "0", inclusive = false)

        assertThat(errorsOf(call, "amount")).containsExactly(
            SingleConstraintError("DecimalMin", "Must be greater than 0")
        )
    }

    @Test
    fun `exclusive DecimalMin accepts values above the bound`() {
        val call = ValidationCall()

        Constraints.checkDecimalMin("amount", BigDecimal("0.0001"), call, value = "0", inclusive = false)

        assertThat(call.finishObject()).isNull()
    }

    @Test
    fun `inclusive DecimalMax accepts the bound`() {
        val call = ValidationCall()

        Constraints.checkDecimalMax("amount", BigDecimal("100.00"), call, value = "100", inclusive = true)

        assertThat(call.finishObject()).isNull()
    }

    @Test
    fun `inclusive DecimalMax rejects values above the bound`() {
        val call = ValidationCall()

        Constraints.checkDecimalMax("amount", BigDecimal("100.01"), call, value = "100", inclusive = true)

        assertThat(errorsOf(call, "amount")).containsExactly(
            SingleConstraintError("DecimalMax", "Must be less than or equal to 100")
        )
    }

    @Test
    fun `exclusive DecimalMax rejects the bound`() {
        val call = ValidationCall()

        Constraints.checkDecimalMax("amount", BigDecimal("100"), call, value = "100", inclusive = false)

        assertThat(errorsOf(call, "amount")).containsExactly(
            SingleConstraintError("DecimalMax", "Must be less than 100")
        )
    }

    @Test
    fun `DecimalMin and DecimalMax default to inclusive`() {
        val call = ValidationCall()

        Constraints.checkDecimalMin("min", BigDecimal.ONE, call, value = "1")
        Constraints.checkDecimalMax("max", BigDecimal.ONE, call, value = "1")

        assertThat(call.finishObject()).isNull()
    }

    @Test
    fun `DecimalMin and DecimalMax support integral, floating point and string values`() {
        val call = ValidationCall()

        Constraints.checkDecimalMin("int", 5, call, value = "5")
        Constraints.checkDecimalMin("long", 5L, call, value = "5.5")
        Constraints.checkDecimalMin("bigInteger", BigInteger.TEN, call, value = "10", inclusive = false)
        Constraints.checkDecimalMax("double", 0.1, call, value = "0.1")
        Constraints.checkDecimalMax("string", "10.5", call, value = "10")
        Constraints.checkDecimalMax("notANumber", "abc", call, value = "10")
        Constraints.checkDecimalMin("nan", Double.NaN, call, value = "0")
        Constraints.checkDecimalMax("negativeInfinity", Double.NEGATIVE_INFINITY, call, value = "0")

        assertThat(errorKeysOf(call)).containsExactly("long", "bigInteger", "string", "notANumber", "nan")
    }

    @Test
    fun `Pattern reuses compiled regex across calls`() {
        val call = ValidationCall()

        Constraints.checkPattern("first", "12-345", call, regexp = "\\d{2}-\\d{3}")
        Constraints.checkPattern("second", "12345", call, regexp = "\\d{2}-\\d{3}")

        assertThat(errorsOf(call, "second")).containsExactly(
            SingleConstraintError("Pattern", "Must match \\d{2}-\\d{3}")
        )
        assertThat(errorKeysOf(call)).containsExactly("second")
    }

}

private fun errorsOf(call: ValidationCall, prop: String): List<SingleConstraintError> {
    val error = call.finishObject()?.properties?.get(prop) as FieldConstraintError?
    return error?.errors.orEmpty()
}

private fun errorKeysOf(call: ValidationCall): Set<String> {
    return call.finishObject()?.properties?.keys.orEmpty()
}
