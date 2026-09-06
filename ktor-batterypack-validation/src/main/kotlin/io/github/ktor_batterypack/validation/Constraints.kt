package io.github.ktor_batterypack.validation

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.temporal.Temporal

object Constraints {

    @JvmStatic
    private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    @JvmStatic
    fun checkNotNull(prop: String, data: Any?, call: ValidationCall) {
        if (data == null) {
            call.propertyError(prop, SingleConstraintError("NotNull"))
        }
    }

    @JvmStatic
    fun checkNotEmpty(prop: String, data: Collection<Any>, call: ValidationCall) {
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
        if (!Regex(regexp).matches(str)) {
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
    fun checkMin(prop: String, num: Int, call: ValidationCall, value: Int) {
        if (num < value) call.propertyError(
            prop,
            SingleConstraintError("Min", "Must be at least $value")
        )
    }

    @JvmStatic
    fun checkMax(prop: String, num: Int, call: ValidationCall, value: Int) {
        if (num > value) call.propertyError(
            prop,
            SingleConstraintError("Max", "Must be at most $value")
        )
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
    fun checkDecimalMin(
        prop: String,
        checkedValue: BigDecimal,
        call: ValidationCall,
        value: String,
        inclusive: Boolean
    ) {
        val comparison = checkedValue.compareTo(value.toBigDecimal())
        if (comparison == -1 || (comparison == 0 && inclusive)) {
            call.propertyError(prop, SingleConstraintError("Min", "Must be greater than $value"))
        }
    }

    @JvmStatic
    fun checkDecimalMax(
        prop: String,
        checkedValue: BigDecimal,
        call: ValidationCall,
        value: String,
        inclusive: Boolean
    ) {
        val comparison = checkedValue.compareTo(value.toBigDecimal())
        if (comparison == 1 || (comparison == 0 && inclusive)) {
            call.propertyError(prop, SingleConstraintError("Max", "Must be greater than $value"))
        }
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

}

private fun Temporal.compareToNow(): Int = when (this) {
    is LocalDate -> compareTo(LocalDate.now())
    is LocalDateTime -> compareTo(LocalDateTime.now())
    is OffsetDateTime -> compareTo(OffsetDateTime.now())
    is ZonedDateTime -> compareTo(ZonedDateTime.now())
    is Instant -> compareTo(Instant.now())
    else -> throw IllegalArgumentException("Unsupported temporal type: ${this::class.simpleName}")
}
