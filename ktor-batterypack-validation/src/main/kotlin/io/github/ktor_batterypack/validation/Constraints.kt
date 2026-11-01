package io.github.ktor_batterypack.validation

import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.temporal.Temporal
import java.util.concurrent.ConcurrentHashMap

object Constraints {

    @JvmStatic
    private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    private val PATTERN_CACHE = ConcurrentHashMap<String, Regex>()

    @JvmStatic
    fun checkNotNull(prop: String, data: Any?, call: ValidationCall) {
        if (data == null) {
            call.propertyError(prop, SingleConstraintError("NotNull", "Must not be null"))
        }
    }

    @JvmStatic
    fun checkNull(prop: String, data: Any?, call: ValidationCall) {
        if (data != null) {
            call.propertyError(prop, SingleConstraintError("Null", "Must be null"))
        }
    }

    @JvmStatic
    fun checkAssertTrue(prop: String, value: Boolean, call: ValidationCall) {
        if (!value) {
            call.propertyError(prop, SingleConstraintError("AssertTrue", "Must be true"))
        }
    }

    @JvmStatic
    fun checkAssertFalse(prop: String, value: Boolean, call: ValidationCall) {
        if (value) {
            call.propertyError(prop, SingleConstraintError("AssertFalse", "Must be false"))
        }
    }

    @JvmStatic
    fun checkNotEmpty(prop: String, data: Collection<*>, call: ValidationCall) {
        if (data.isEmpty()) {
            call.propertyError(prop, SingleConstraintError("NotEmpty"))
        }
    }

    @JvmStatic
    fun checkNotEmpty(prop: String, data: Array<*>, call: ValidationCall) {
        if (data.isEmpty()) {
            call.propertyError(prop, SingleConstraintError("NotEmpty"))
        }
    }

    @JvmStatic
    fun checkNotEmpty(prop: String, data: Map<*, *>, call: ValidationCall) {
        if (data.isEmpty()) {
            call.propertyError(prop, SingleConstraintError("NotEmpty"))
        }
    }

    @JvmStatic
    fun checkNotEmpty(prop: String, data: String, call: ValidationCall) {
        if (data.isEmpty()) {
            call.propertyError(prop, SingleConstraintError("NotEmpty"))
        }
    }

    @JvmStatic
    fun checkNotBlank(prop: String, data: String, call: ValidationCall) {
        if (data.isBlank()) {
            call.propertyError(prop, SingleConstraintError("NotBlank"))
        }
    }

    @JvmStatic
    fun checkPast(prop: String, date: Temporal, call: ValidationCall) {
        if (date.compareToNow() >= 0) {
            call.propertyError(prop, SingleConstraintError("Past"))
        }
    }

    @JvmStatic
    fun checkPastOrPresent(prop: String, date: Temporal, call: ValidationCall) {
        if (date.compareToNow() > 0) {
            call.propertyError(prop, SingleConstraintError("PastOrPresent"))
        }
    }

    @JvmStatic
    fun checkFuture(prop: String, date: Temporal, call: ValidationCall) {
        if (date.compareToNow() <= 0) {
            call.propertyError(prop, SingleConstraintError("Future"))
        }
    }

    @JvmStatic
    fun checkFutureOrPresent(prop: String, date: Temporal, call: ValidationCall) {
        if (date.compareToNow() < 0) {
            call.propertyError(prop, SingleConstraintError("FutureOrPresent"))
        }
    }

    @JvmStatic
    fun checkPattern(prop: String, str: String, call: ValidationCall, regexp: String) {
        val pattern = PATTERN_CACHE.computeIfAbsent(regexp) { Regex(it) }
        if (!pattern.matches(str)) {
            call.propertyError(prop, SingleConstraintError("Pattern", "Must match $regexp"))
        }
    }

    @JvmStatic
    fun checkEmail(
        prop: String,
        str: String,
        call: ValidationCall,
        @Suppress("unused") regexp: String
    ) {
        if (!EMAIL_PATTERN.matches(str)) {
            call.propertyError(
                prop,
                SingleConstraintError("Email", "Must be a well-formed email address")
            )
        }
    }

    @JvmStatic
    fun checkSize(prop: String, data: String, call: ValidationCall, min: Int, max: Int) {
        if (data.length < min) {
            call.propertyError(prop, SingleConstraintError("Size", "Must be longer than min=$min"))
        }
        if (data.length > max) {
            call.propertyError(prop, SingleConstraintError("Size", "Must be shorter than max=$max"))
        }
    }

    @JvmStatic
    fun checkMin(prop: String, num: Int, call: ValidationCall, value: Long) {
        checkMin(prop, num.toLong(), call, value)
    }

    @JvmStatic
    fun checkMax(prop: String, num: Int, call: ValidationCall, value: Long) {
        checkMax(prop, num.toLong(), call, value)
    }

    @JvmStatic
    fun checkMin(prop: String, num: Short, call: ValidationCall, value: Long) {
        checkMin(prop, num.toLong(), call, value)
    }

    @JvmStatic
    fun checkMax(prop: String, num: Short, call: ValidationCall, value: Long) {
        checkMax(prop, num.toLong(), call, value)
    }

    @JvmStatic
    fun checkMin(prop: String, num: Byte, call: ValidationCall, value: Long) {
        checkMin(prop, num.toLong(), call, value)
    }

    @JvmStatic
    fun checkMax(prop: String, num: Byte, call: ValidationCall, value: Long) {
        checkMax(prop, num.toLong(), call, value)
    }

    @JvmStatic
    fun checkMin(prop: String, num: BigInteger, call: ValidationCall, value: Long) {
        if (num < BigInteger.valueOf(value)) call.propertyError(
            prop,
            SingleConstraintError("Min", "Must be at least $value")
        )
    }

    @JvmStatic
    fun checkMax(prop: String, num: BigInteger, call: ValidationCall, value: Long) {
        if (num > BigInteger.valueOf(value)) call.propertyError(
            prop,
            SingleConstraintError("Max", "Must be at most $value")
        )
    }

    @JvmStatic
    fun checkMin(prop: String, num: BigDecimal, call: ValidationCall, value: Long) {
        if (num < BigDecimal.valueOf(value)) call.propertyError(
            prop,
            SingleConstraintError("Min", "Must be at least $value")
        )
    }

    @JvmStatic
    fun checkMax(prop: String, num: BigDecimal, call: ValidationCall, value: Long) {
        if (num > BigDecimal.valueOf(value)) call.propertyError(
            prop,
            SingleConstraintError("Max", "Must be at most $value")
        )
    }

    @JvmStatic
    fun checkMin(prop: String, num: Float, call: ValidationCall, value: Long) {
        checkMin(prop, num.toDouble(), call, value)
    }

    @JvmStatic
    fun checkMax(prop: String, num: Float, call: ValidationCall, value: Long) {
        checkMax(prop, num.toDouble(), call, value)
    }

    @JvmStatic
    fun checkMin(prop: String, num: Long, call: ValidationCall, value: Long) {
        if (num < value) call.propertyError(
            prop,
            SingleConstraintError("Min", "Must be at least $value")
        )
    }

    @JvmStatic
    fun checkMax(prop: String, num: Long, call: ValidationCall, value: Long) {
        if (num > value) call.propertyError(
            prop,
            SingleConstraintError("Max", "Must be at most $value")
        )
    }

    @JvmStatic
    fun checkMin(prop: String, num: Double, call: ValidationCall, value: Long) {
        if (num < value) call.propertyError(
            prop,
            SingleConstraintError("Min", "Must be at least $value")
        )
    }

    @JvmStatic
    fun checkMax(prop: String, num: Double, call: ValidationCall, value: Long) {
        if (num > value) call.propertyError(
            prop,
            SingleConstraintError("Max", "Must be at most $value")
        )
    }

    @JvmStatic
    fun checkPositive(prop: String, value: BigDecimal, call: ValidationCall) {
        if (value <= BigDecimal.ZERO) {
            call.propertyError(prop, SingleConstraintError("Positive"))
        }
    }

    @JvmStatic
    fun checkPositive(prop: String, value: Int, call: ValidationCall) {
        if (value <= 0) {
            call.propertyError(prop, SingleConstraintError("Positive"))
        }
    }

    @JvmStatic
    fun checkPositive(prop: String, value: Long, call: ValidationCall) {
        if (value <= 0) {
            call.propertyError(prop, SingleConstraintError("Positive"))
        }
    }

    @JvmStatic
    fun checkPositive(prop: String, value: Double, call: ValidationCall) {
        if (value <= 0.0) {
            call.propertyError(prop, SingleConstraintError("Positive"))
        }
    }

    @JvmStatic
    fun checkPositive(prop: String, value: Float, call: ValidationCall) {
        checkPositive(prop, value.toDouble(), call)
    }

    @JvmStatic
    fun checkPositive(prop: String, value: Short, call: ValidationCall) {
        checkPositive(prop, value.toLong(), call)
    }

    @JvmStatic
    fun checkPositive(prop: String, value: Byte, call: ValidationCall) {
        checkPositive(prop, value.toLong(), call)
    }

    @JvmStatic
    fun checkPositive(prop: String, value: BigInteger, call: ValidationCall) {
        if (value.signum() <= 0) {
            call.propertyError(prop, SingleConstraintError("Positive"))
        }
    }

    @JvmStatic
    fun checkPositiveOrZero(prop: String, value: BigDecimal, call: ValidationCall) {
        if (value < BigDecimal.ZERO) {
            call.propertyError(prop, SingleConstraintError("PositiveOrZero"))
        }
    }

    @JvmStatic
    fun checkPositiveOrZero(prop: String, value: Int, call: ValidationCall) {
        if (value < 0) {
            call.propertyError(prop, SingleConstraintError("PositiveOrZero"))
        }
    }

    @JvmStatic
    fun checkPositiveOrZero(prop: String, value: Long, call: ValidationCall) {
        if (value < 0) {
            call.propertyError(prop, SingleConstraintError("PositiveOrZero"))
        }
    }

    @JvmStatic
    fun checkPositiveOrZero(prop: String, value: Double, call: ValidationCall) {
        if (value < 0.0) {
            call.propertyError(prop, SingleConstraintError("PositiveOrZero"))
        }
    }

    @JvmStatic
    fun checkPositiveOrZero(prop: String, value: Float, call: ValidationCall) {
        checkPositiveOrZero(prop, value.toDouble(), call)
    }

    @JvmStatic
    fun checkPositiveOrZero(prop: String, value: Short, call: ValidationCall) {
        checkPositiveOrZero(prop, value.toLong(), call)
    }

    @JvmStatic
    fun checkPositiveOrZero(prop: String, value: Byte, call: ValidationCall) {
        checkPositiveOrZero(prop, value.toLong(), call)
    }

    @JvmStatic
    fun checkPositiveOrZero(prop: String, value: BigInteger, call: ValidationCall) {
        if (value.signum() < 0) {
            call.propertyError(prop, SingleConstraintError("PositiveOrZero"))
        }
    }

    @JvmStatic
    fun checkNegative(prop: String, value: BigDecimal, call: ValidationCall) {
        if (value >= BigDecimal.ZERO) {
            call.propertyError(prop, SingleConstraintError("Negative"))
        }
    }

    @JvmStatic
    fun checkNegative(prop: String, value: Int, call: ValidationCall) {
        if (value >= 0) {
            call.propertyError(prop, SingleConstraintError("Negative"))
        }
    }

    @JvmStatic
    fun checkNegative(prop: String, value: Long, call: ValidationCall) {
        if (value >= 0) {
            call.propertyError(prop, SingleConstraintError("Negative"))
        }
    }

    @JvmStatic
    fun checkNegative(prop: String, value: Double, call: ValidationCall) {
        if (value >= 0.0) {
            call.propertyError(prop, SingleConstraintError("Negative"))
        }
    }

    @JvmStatic
    fun checkNegative(prop: String, value: Float, call: ValidationCall) {
        checkNegative(prop, value.toDouble(), call)
    }

    @JvmStatic
    fun checkNegative(prop: String, value: Short, call: ValidationCall) {
        checkNegative(prop, value.toLong(), call)
    }

    @JvmStatic
    fun checkNegative(prop: String, value: Byte, call: ValidationCall) {
        checkNegative(prop, value.toLong(), call)
    }

    @JvmStatic
    fun checkNegative(prop: String, value: BigInteger, call: ValidationCall) {
        if (value.signum() >= 0) {
            call.propertyError(prop, SingleConstraintError("Negative"))
        }
    }

    @JvmStatic
    fun checkNegativeOrZero(prop: String, value: BigDecimal, call: ValidationCall) {
        if (value > BigDecimal.ZERO) {
            call.propertyError(prop, SingleConstraintError("NegativeOrZero"))
        }
    }

    @JvmStatic
    fun checkNegativeOrZero(prop: String, value: Int, call: ValidationCall) {
        if (value > 0) {
            call.propertyError(prop, SingleConstraintError("NegativeOrZero"))
        }
    }

    @JvmStatic
    fun checkNegativeOrZero(prop: String, value: Long, call: ValidationCall) {
        if (value > 0) {
            call.propertyError(prop, SingleConstraintError("NegativeOrZero"))
        }
    }

    @JvmStatic
    fun checkNegativeOrZero(prop: String, value: Double, call: ValidationCall) {
        if (value > 0.0) {
            call.propertyError(prop, SingleConstraintError("NegativeOrZero"))
        }
    }

    @JvmStatic
    fun checkNegativeOrZero(prop: String, value: Float, call: ValidationCall) {
        checkNegativeOrZero(prop, value.toDouble(), call)
    }

    @JvmStatic
    fun checkNegativeOrZero(prop: String, value: Short, call: ValidationCall) {
        checkNegativeOrZero(prop, value.toLong(), call)
    }

    @JvmStatic
    fun checkNegativeOrZero(prop: String, value: Byte, call: ValidationCall) {
        checkNegativeOrZero(prop, value.toLong(), call)
    }

    @JvmStatic
    fun checkNegativeOrZero(prop: String, value: BigInteger, call: ValidationCall) {
        if (value.signum() > 0) {
            call.propertyError(prop, SingleConstraintError("NegativeOrZero"))
        }
    }

    @JvmStatic
    fun checkDecimalMin(
        prop: String,
        checkedValue: BigDecimal,
        call: ValidationCall,
        value: String,
        inclusive: Boolean = true
    ) {
        val comparison = checkedValue.compareTo(value.toBigDecimal())
        if (comparison < 0 || (comparison == 0 && !inclusive)) {
            decimalMinError(prop, call, value, inclusive)
        }
    }

    @JvmStatic
    fun checkDecimalMin(prop: String, checkedValue: BigInteger, call: ValidationCall, value: String, inclusive: Boolean = true) {
        checkDecimalMin(prop, checkedValue.toBigDecimal(), call, value, inclusive)
    }

    @JvmStatic
    fun checkDecimalMin(prop: String, checkedValue: Int, call: ValidationCall, value: String, inclusive: Boolean = true) {
        checkDecimalMin(prop, checkedValue.toBigDecimal(), call, value, inclusive)
    }

    @JvmStatic
    fun checkDecimalMin(prop: String, checkedValue: Long, call: ValidationCall, value: String, inclusive: Boolean = true) {
        checkDecimalMin(prop, checkedValue.toBigDecimal(), call, value, inclusive)
    }

    @JvmStatic
    fun checkDecimalMin(prop: String, checkedValue: Double, call: ValidationCall, value: String, inclusive: Boolean = true) {
        if (checkedValue.isNaN() || checkedValue == Double.NEGATIVE_INFINITY) {
            decimalMinError(prop, call, value, inclusive)
            return
        }
        if (checkedValue == Double.POSITIVE_INFINITY) {
            return
        }
        checkDecimalMin(prop, checkedValue.toBigDecimal(), call, value, inclusive)
    }

    @JvmStatic
    fun checkDecimalMin(prop: String, checkedValue: Float, call: ValidationCall, value: String, inclusive: Boolean = true) {
        if (checkedValue.isNaN() || checkedValue == Float.NEGATIVE_INFINITY) {
            decimalMinError(prop, call, value, inclusive)
            return
        }
        if (checkedValue == Float.POSITIVE_INFINITY) {
            return
        }
        checkDecimalMin(prop, checkedValue.toBigDecimal(), call, value, inclusive)
    }

    @JvmStatic
    fun checkDecimalMin(prop: String, checkedValue: Short, call: ValidationCall, value: String, inclusive: Boolean = true) {
        checkDecimalMin(prop, checkedValue.toLong().toBigDecimal(), call, value, inclusive)
    }

    @JvmStatic
    fun checkDecimalMin(prop: String, checkedValue: Byte, call: ValidationCall, value: String, inclusive: Boolean = true) {
        checkDecimalMin(prop, checkedValue.toLong().toBigDecimal(), call, value, inclusive)
    }

    @JvmStatic
    fun checkDecimalMin(prop: String, checkedValue: String, call: ValidationCall, value: String, inclusive: Boolean = true) {
        val number = checkedValue.toBigDecimalOrNull()
        if (number == null) {
            decimalMinError(prop, call, value, inclusive)
            return
        }
        checkDecimalMin(prop, number, call, value, inclusive)
    }

    @JvmStatic
    fun checkDecimalMax(
        prop: String,
        checkedValue: BigDecimal,
        call: ValidationCall,
        value: String,
        inclusive: Boolean = true
    ) {
        val comparison = checkedValue.compareTo(value.toBigDecimal())
        if (comparison > 0 || (comparison == 0 && !inclusive)) {
            decimalMaxError(prop, call, value, inclusive)
        }
    }

    @JvmStatic
    fun checkDecimalMax(prop: String, checkedValue: BigInteger, call: ValidationCall, value: String, inclusive: Boolean = true) {
        checkDecimalMax(prop, checkedValue.toBigDecimal(), call, value, inclusive)
    }

    @JvmStatic
    fun checkDecimalMax(prop: String, checkedValue: Int, call: ValidationCall, value: String, inclusive: Boolean = true) {
        checkDecimalMax(prop, checkedValue.toBigDecimal(), call, value, inclusive)
    }

    @JvmStatic
    fun checkDecimalMax(prop: String, checkedValue: Long, call: ValidationCall, value: String, inclusive: Boolean = true) {
        checkDecimalMax(prop, checkedValue.toBigDecimal(), call, value, inclusive)
    }

    @JvmStatic
    fun checkDecimalMax(prop: String, checkedValue: Double, call: ValidationCall, value: String, inclusive: Boolean = true) {
        if (checkedValue.isNaN() || checkedValue == Double.POSITIVE_INFINITY) {
            decimalMaxError(prop, call, value, inclusive)
            return
        }
        if (checkedValue == Double.NEGATIVE_INFINITY) {
            return
        }
        checkDecimalMax(prop, checkedValue.toBigDecimal(), call, value, inclusive)
    }

    @JvmStatic
    fun checkDecimalMax(prop: String, checkedValue: Float, call: ValidationCall, value: String, inclusive: Boolean = true) {
        if (checkedValue.isNaN() || checkedValue == Float.POSITIVE_INFINITY) {
            decimalMaxError(prop, call, value, inclusive)
            return
        }
        if (checkedValue == Float.NEGATIVE_INFINITY) {
            return
        }
        checkDecimalMax(prop, checkedValue.toBigDecimal(), call, value, inclusive)
    }

    @JvmStatic
    fun checkDecimalMax(prop: String, checkedValue: Short, call: ValidationCall, value: String, inclusive: Boolean = true) {
        checkDecimalMax(prop, checkedValue.toLong().toBigDecimal(), call, value, inclusive)
    }

    @JvmStatic
    fun checkDecimalMax(prop: String, checkedValue: Byte, call: ValidationCall, value: String, inclusive: Boolean = true) {
        checkDecimalMax(prop, checkedValue.toLong().toBigDecimal(), call, value, inclusive)
    }

    @JvmStatic
    fun checkDecimalMax(prop: String, checkedValue: String, call: ValidationCall, value: String, inclusive: Boolean = true) {
        val number = checkedValue.toBigDecimalOrNull()
        if (number == null) {
            decimalMaxError(prop, call, value, inclusive)
            return
        }
        checkDecimalMax(prop, number, call, value, inclusive)
    }

    @JvmStatic
    fun checkSize(prop: String, data: Collection<*>, call: ValidationCall, max: Int, min: Int) {
        if (data.size !in min..max) {
            call.propertyError(
                prop,
                SingleConstraintError("Size", "Must have size between [$min, $max]")
            )
        }
    }

    @JvmStatic
    fun checkSize(prop: String, data: Array<*>, call: ValidationCall, max: Int, min: Int) {
        if (data.size !in min..max) {
            call.propertyError(
                prop,
                SingleConstraintError("Size", "Must have size between [$min, $max]")
            )
        }
    }

    @JvmStatic
    fun checkSize(prop: String, data: Map<*, *>, call: ValidationCall, max: Int, min: Int) {
        if (data.size !in min..max) {
            call.propertyError(
                prop,
                SingleConstraintError("Size", "Must have size between [$min, $max]")
            )
        }
    }

    @JvmStatic
    fun checkDigits(prop: String, value: BigDecimal, call: ValidationCall, integer: Int, fraction: Int) {
        val stripped = value.stripTrailingZeros()
        val integerDigits = stripped.precision() - stripped.scale()
        val fractionDigits = maxOf(stripped.scale(), 0)
        if (integerDigits > integer || fractionDigits > fraction) {
            digitsError(prop, call, integer, fraction)
        }
    }

    @JvmStatic
    fun checkDigits(prop: String, value: BigInteger, call: ValidationCall, integer: Int, fraction: Int) {
        checkDigits(prop, value.toBigDecimal(), call, integer, fraction)
    }

    @JvmStatic
    fun checkDigits(prop: String, value: Int, call: ValidationCall, integer: Int, fraction: Int) {
        checkDigits(prop, value.toBigDecimal(), call, integer, fraction)
    }

    @JvmStatic
    fun checkDigits(prop: String, value: Long, call: ValidationCall, integer: Int, fraction: Int) {
        checkDigits(prop, value.toBigDecimal(), call, integer, fraction)
    }

    @JvmStatic
    fun checkDigits(prop: String, value: Double, call: ValidationCall, integer: Int, fraction: Int) {
        if (value.isNaN() || value.isInfinite()) {
            digitsError(prop, call, integer, fraction)
            return
        }
        checkDigits(prop, value.toString().toBigDecimal(), call, integer, fraction)
    }

    @JvmStatic
    fun checkDigits(prop: String, value: Float, call: ValidationCall, integer: Int, fraction: Int) {
        if (value.isNaN() || value.isInfinite()) {
            digitsError(prop, call, integer, fraction)
            return
        }
        checkDigits(prop, value.toString().toBigDecimal(), call, integer, fraction)
    }

    @JvmStatic
    fun checkDigits(prop: String, value: Short, call: ValidationCall, integer: Int, fraction: Int) {
        checkDigits(prop, value.toLong().toBigDecimal(), call, integer, fraction)
    }

    @JvmStatic
    fun checkDigits(prop: String, value: Byte, call: ValidationCall, integer: Int, fraction: Int) {
        checkDigits(prop, value.toLong().toBigDecimal(), call, integer, fraction)
    }

    @JvmStatic
    fun checkDigits(prop: String, value: String, call: ValidationCall, integer: Int, fraction: Int) {
        val number = value.toBigDecimalOrNull()
        if (number == null) {
            digitsError(prop, call, integer, fraction)
            return
        }
        checkDigits(prop, number, call, integer, fraction)
    }

}

private fun digitsError(prop: String, call: ValidationCall, integer: Int, fraction: Int) {
    call.propertyError(
        prop,
        SingleConstraintError("Digits", "Must have at most $integer integer digits and $fraction fraction digits")
    )
}

private fun decimalMinError(prop: String, call: ValidationCall, value: String, inclusive: Boolean) {
    val bound = if (inclusive) "greater than or equal to" else "greater than"
    call.propertyError(prop, SingleConstraintError("DecimalMin", "Must be $bound $value"))
}

private fun decimalMaxError(prop: String, call: ValidationCall, value: String, inclusive: Boolean) {
    val bound = if (inclusive) "less than or equal to" else "less than"
    call.propertyError(prop, SingleConstraintError("DecimalMax", "Must be $bound $value"))
}

private fun Temporal.compareToNow(): Int = when (this) {
    is LocalDate -> compareTo(LocalDate.now())
    is LocalDateTime -> compareTo(LocalDateTime.now())
    is OffsetDateTime -> compareTo(OffsetDateTime.now())
    is ZonedDateTime -> compareTo(ZonedDateTime.now())
    is Instant -> compareTo(Instant.now())
    else -> throw IllegalArgumentException("Unsupported temporal type: ${this::class.simpleName}")
}
