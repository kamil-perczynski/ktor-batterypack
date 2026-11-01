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

    @Test
    fun `NotNull and Null check presence`() {
        val call = ValidationCall()

        Constraints.checkNotNull("present", "x", call)
        Constraints.checkNotNull("missing", null, call)
        Constraints.checkNull("unset", null, call)
        Constraints.checkNull("set", "x", call)

        val errors = call.finishObject()?.properties
        assertThat(errors?.keys).containsExactly("missing", "set")
        assertThat((errors?.get("missing") as FieldConstraintError).errors)
            .containsExactly(SingleConstraintError("NotNull", "Must not be null"))
        assertThat((errors.get("set") as FieldConstraintError).errors)
            .containsExactly(SingleConstraintError("Null", "Must be null"))
    }

    @Test
    fun `AssertTrue and AssertFalse check the boolean value`() {
        val call = ValidationCall()

        Constraints.checkAssertTrue("accepted", true, call)
        Constraints.checkAssertTrue("declined", false, call)
        Constraints.checkAssertFalse("clean", false, call)
        Constraints.checkAssertFalse("dirty", true, call)

        val errors = call.finishObject()?.properties
        assertThat(errors?.keys).containsExactly("declined", "dirty")
        assertThat((errors?.get("declined") as FieldConstraintError).errors)
            .containsExactly(SingleConstraintError("AssertTrue", "Must be true"))
        assertThat((errors.get("dirty") as FieldConstraintError).errors)
            .containsExactly(SingleConstraintError("AssertFalse", "Must be false"))
    }

    @Test
    fun `Digits counts integer and fraction digits without trailing zeros`() {
        val call = ValidationCall()

        Constraints.checkDigits("fits", BigDecimal("999.99"), call, integer = 3, fraction = 2)
        Constraints.checkDigits("trailingZeros", BigDecimal("1.5000"), call, integer = 1, fraction = 1)
        Constraints.checkDigits("bigInteger", BigInteger("1000"), call, integer = 4, fraction = 0)
        Constraints.checkDigits("fractionOnly", 0.25, call, integer = 0, fraction = 2)
        Constraints.checkDigits("tooManyIntegers", 1000, call, integer = 3, fraction = 0)
        Constraints.checkDigits("tooManyFractions", BigDecimal("1.234"), call, integer = 3, fraction = 2)
        Constraints.checkDigits("long", 12_345L, call, integer = 4, fraction = 0)
        Constraints.checkDigits("notANumber", "abc", call, integer = 3, fraction = 0)
        Constraints.checkDigits("nan", Double.NaN, call, integer = 3, fraction = 0)

        assertThat(errorKeysOf(call)).containsExactly("tooManyIntegers", "tooManyFractions", "long", "notANumber", "nan")
    }

    @Test
    fun `Digits reports the allowed digits`() {
        val call = ValidationCall()

        Constraints.checkDigits("amount", BigDecimal("1234.5"), call, integer = 3, fraction = 2)

        assertThat(errorsOf(call, "amount")).containsExactly(
            SingleConstraintError("Digits", "Must have at most 3 integer digits and 2 fraction digits")
        )
    }

    @Test
    fun `NotEmpty and Size support maps`() {
        val call = ValidationCall()

        Constraints.checkNotEmpty("filled", mapOf("a" to 1), call)
        Constraints.checkNotEmpty("empty", emptyMap<String, Int>(), call)
        Constraints.checkSize("small", mapOf("a" to 1), call, max = 1, min = 0)
        Constraints.checkSize("large", mapOf("a" to 1, "b" to 2), call, max = 1, min = 0)

        val errors = call.finishObject()?.properties
        assertThat(errors?.keys).containsExactly("empty", "large")
        assertThat((errors?.get("large") as FieldConstraintError).errors)
            .containsExactly(SingleConstraintError("Size", "Must have size between [0, 1]"))
    }

    @Test
    fun `NotEmpty accepts collections of nullable items`() {
        val call = ValidationCall()

        Constraints.checkNotEmpty("items", listOf<String?>(null), call)

        assertThat(call.finishObject()).isNull()
    }

    @Test
    fun `NotEmpty and Size support arrays`() {
        val call = ValidationCall()

        Constraints.checkNotEmpty("filled", arrayOf("a"), call)
        Constraints.checkNotEmpty("empty", emptyArray<String>(), call)
        Constraints.checkSize("small", arrayOf("a"), call, max = 1, min = 0)
        Constraints.checkSize("large", arrayOf("a", "b"), call, max = 1, min = 0)

        assertThat(errorKeysOf(call)).containsExactly("empty", "large")
    }

    @Test
    fun `Min and Max accept bounds beyond the int range`() {
        val call = ValidationCall()

        Constraints.checkMin("int", Int.MAX_VALUE, call, value = 3_000_000_000)
        Constraints.checkMax("long", 3_000_000_000, call, value = 3_000_000_000)
        Constraints.checkMax("big", BigInteger("3000000001"), call, value = 3_000_000_000)

        assertThat(errorKeysOf(call)).containsExactly("int", "big")
    }

    @Test
    fun `Min and Max support every numeric type`() {
        val call = ValidationCall()

        Constraints.checkMin("short", 1.toShort(), call, value = 2)
        Constraints.checkMin("byte", 2.toByte(), call, value = 2)
        Constraints.checkMin("float", 1.5f, call, value = 2)
        Constraints.checkMin("decimal", BigDecimal("1.99"), call, value = 2)
        Constraints.checkMin("integer", BigInteger.TWO, call, value = 2)
        Constraints.checkMax("shortMax", 3.toShort(), call, value = 2)
        Constraints.checkMax("byteMax", 2.toByte(), call, value = 2)
        Constraints.checkMax("floatMax", 2.5f, call, value = 2)
        Constraints.checkMax("decimalMax", BigDecimal("2.00"), call, value = 2)
        Constraints.checkMax("integerMax", BigInteger.valueOf(3), call, value = 2)

        assertThat(errorKeysOf(call)).containsExactly("short", "float", "decimal", "shortMax", "floatMax", "integerMax")
    }

    @Test
    fun `Min reports the bound`() {
        val call = ValidationCall()

        Constraints.checkMin("count", 1, call, value = 2)

        assertThat(errorsOf(call, "count")).containsExactly(SingleConstraintError("Min", "Must be at least 2"))
    }

    @Test
    fun `sign constraints support every numeric type`() {
        val call = ValidationCall()

        Constraints.checkPositive("positiveFloat", 0f, call)
        Constraints.checkPositive("positiveShort", 1.toShort(), call)
        Constraints.checkPositive("positiveByte", 0.toByte(), call)
        Constraints.checkPositive("positiveInteger", BigInteger.ONE, call)
        Constraints.checkPositiveOrZero("positiveOrZeroFloat", -0.5f, call)
        Constraints.checkPositiveOrZero("positiveOrZeroShort", 0.toShort(), call)
        Constraints.checkPositiveOrZero("positiveOrZeroByte", (-1).toByte(), call)
        Constraints.checkPositiveOrZero("positiveOrZeroInteger", BigInteger.ZERO, call)
        Constraints.checkNegative("negativeFloat", -0.5f, call)
        Constraints.checkNegative("negativeShort", 0.toShort(), call)
        Constraints.checkNegative("negativeByte", (-1).toByte(), call)
        Constraints.checkNegative("negativeInteger", BigInteger.ZERO, call)
        Constraints.checkNegativeOrZero("negativeOrZeroFloat", 0.5f, call)
        Constraints.checkNegativeOrZero("negativeOrZeroShort", 0.toShort(), call)
        Constraints.checkNegativeOrZero("negativeOrZeroByte", 1.toByte(), call)
        Constraints.checkNegativeOrZero("negativeOrZeroInteger", BigInteger.ZERO, call)

        assertThat(errorKeysOf(call)).containsExactly(
            "positiveFloat",
            "positiveByte",
            "positiveOrZeroFloat",
            "positiveOrZeroByte",
            "negativeShort",
            "negativeInteger",
            "negativeOrZeroFloat",
            "negativeOrZeroByte"
        )
    }

    @Test
    fun `DecimalMin, DecimalMax and Digits support short, byte and float`() {
        val call = ValidationCall()

        Constraints.checkDecimalMin("short", 1.toShort(), call, value = "2")
        Constraints.checkDecimalMax("byte", 3.toByte(), call, value = "2")
        Constraints.checkDecimalMin("float", 0.1f, call, value = "0.1")
        Constraints.checkDecimalMax("floatNaN", Float.NaN, call, value = "1")
        Constraints.checkDigits("floatDigits", 0.1f, call, integer = 1, fraction = 1)
        Constraints.checkDigits("shortDigits", 123.toShort(), call, integer = 2, fraction = 0)
        Constraints.checkDigits("byteDigits", 12.toByte(), call, integer = 2, fraction = 0)

        assertThat(errorKeysOf(call)).containsExactly("short", "byte", "floatNaN", "shortDigits")
    }

}

private fun errorsOf(call: ValidationCall, prop: String): List<SingleConstraintError> {
    val error = call.finishObject()?.properties?.get(prop) as FieldConstraintError?
    return error?.errors.orEmpty()
}

private fun errorKeysOf(call: ValidationCall): Set<String> {
    return call.finishObject()?.properties?.keys.orEmpty()
}
