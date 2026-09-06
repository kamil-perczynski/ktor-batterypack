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

    fun checkNotNull(prop: String, data: Any?, call: ValidationCall) {
        if (data == null) {
            call.propertyError(prop, SingleConstraintError("NotNull"))
        }
    }

    fun checkNotEmpty(prop: String, data: Collection<Any>, call: ValidationCall) {
        if (data.isEmpty()) {
            call.propertyError(prop, SingleConstraintError("NotEmpty"))
        }
    }

    fun checkNotEmpty(prop: String, data: String, call: ValidationCall) {
        if (data.isEmpty()) {
            call.propertyError(prop, SingleConstraintError("NotEmpty"))
        }
    }

    fun checkNotBlank(prop: String, data: String, call: ValidationCall) {
        if (data.isBlank()) {
            call.propertyError(prop, SingleConstraintError("NotBlank"))
        }
    }

    fun checkPast(prop: String, date: Temporal, call: ValidationCall) {
        if (date.compareToNow() >= 0) {
            call.propertyError(prop, SingleConstraintError("Past"))
        }
    }

    fun checkPastOrPresent(prop: String, date: Temporal, call: ValidationCall) {
        if (date.compareToNow() > 0) {
            call.propertyError(prop, SingleConstraintError("PastOrPresent"))
        }
    }

    fun checkFuture(prop: String, date: Temporal, call: ValidationCall) {
        if (date.compareToNow() <= 0) {
            call.propertyError(prop, SingleConstraintError("Future"))
        }
    }

    fun checkFutureOrPresent(prop: String, date: Temporal, call: ValidationCall) {
        if (date.compareToNow() < 0) {
            call.propertyError(prop, SingleConstraintError("FutureOrPresent"))
        }
    }

    fun checkPattern(prop: String, str: String, call: ValidationCall, regexp: String) {
        if (!Regex(regexp).matches(str)) {
            call.propertyError(prop, SingleConstraintError("Pattern", "Must match $regexp"))
        }
    }

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

    fun checkSize(prop: String, data: String, call: ValidationCall, min: Int, max: Int) {
        if (data.length < min) {
            call.propertyError(prop, SingleConstraintError("Size", "Must be longer than min=$min"))
        }
        if (data.length > max) {
            call.propertyError(prop, SingleConstraintError("Size", "Must be shorter than max=$max"))
        }
    }

    fun checkMin(prop: String, num: Int, call: ValidationCall, value: Int) {
        if (num < value) call.propertyError(
            prop,
            SingleConstraintError("Min", "Must be at least $value")
        )
    }

    fun checkMax(prop: String, num: Int, call: ValidationCall, value: Int) {
        if (num > value) call.propertyError(
            prop,
            SingleConstraintError("Max", "Must be at most $value")
        )
    }

    fun checkMin(prop: String, num: Long, call: ValidationCall, value: Long) {
        if (num < value) call.propertyError(
            prop,
            SingleConstraintError("Min", "Must be at least $value")
        )
    }

    fun checkMax(prop: String, num: Long, call: ValidationCall, value: Long) {
        if (num > value) call.propertyError(
            prop,
            SingleConstraintError("Max", "Must be at most $value")
        )
    }

    fun checkMin(prop: String, num: Double, call: ValidationCall, value: Long) {
        if (num < value) call.propertyError(
            prop,
            SingleConstraintError("Min", "Must be at least $value")
        )
    }

    fun checkMax(prop: String, num: Double, call: ValidationCall, value: Long) {
        if (num > value) call.propertyError(
            prop,
            SingleConstraintError("Max", "Must be at most $value")
        )
    }

    fun checkPositive(prop: String, value: BigDecimal, call: ValidationCall) {
        if (value <= BigDecimal.ZERO) {
            call.propertyError(prop, SingleConstraintError("Positive"))
        }
    }

    fun checkPositive(prop: String, value: Int, call: ValidationCall) {
        if (value <= 0) {
            call.propertyError(prop, SingleConstraintError("Positive"))
        }
    }

    fun checkPositive(prop: String, value: Long, call: ValidationCall) {
        if (value <= 0) {
            call.propertyError(prop, SingleConstraintError("Positive"))
        }
    }

    fun checkPositive(prop: String, value: Double, call: ValidationCall) {
        if (value <= 0.0) {
            call.propertyError(prop, SingleConstraintError("Positive"))
        }
    }

    fun checkPositiveOrZero(prop: String, value: BigDecimal, call: ValidationCall) {
        if (value < BigDecimal.ZERO) {
            call.propertyError(prop, SingleConstraintError("PositiveOrZero"))
        }
    }

    fun checkPositiveOrZero(prop: String, value: Int, call: ValidationCall) {
        if (value < 0) {
            call.propertyError(prop, SingleConstraintError("PositiveOrZero"))
        }
    }

    fun checkPositiveOrZero(prop: String, value: Long, call: ValidationCall) {
        if (value < 0) {
            call.propertyError(prop, SingleConstraintError("PositiveOrZero"))
        }
    }

    fun checkPositiveOrZero(prop: String, value: Double, call: ValidationCall) {
        if (value < 0.0) {
            call.propertyError(prop, SingleConstraintError("PositiveOrZero"))
        }
    }

    fun checkNegative(prop: String, value: BigDecimal, call: ValidationCall) {
        if (value >= BigDecimal.ZERO) {
            call.propertyError(prop, SingleConstraintError("Negative"))
        }
    }

    fun checkNegative(prop: String, value: Int, call: ValidationCall) {
        if (value >= 0) {
            call.propertyError(prop, SingleConstraintError("Negative"))
        }
    }

    fun checkNegative(prop: String, value: Long, call: ValidationCall) {
        if (value >= 0) {
            call.propertyError(prop, SingleConstraintError("Negative"))
        }
    }

    fun checkNegative(prop: String, value: Double, call: ValidationCall) {
        if (value >= 0.0) {
            call.propertyError(prop, SingleConstraintError("Negative"))
        }
    }

    fun checkNegativeOrZero(prop: String, value: BigDecimal, call: ValidationCall) {
        if (value > BigDecimal.ZERO) {
            call.propertyError(prop, SingleConstraintError("NegativeOrZero"))
        }
    }

    fun checkNegativeOrZero(prop: String, value: Int, call: ValidationCall) {
        if (value > 0) {
            call.propertyError(prop, SingleConstraintError("NegativeOrZero"))
        }
    }

    fun checkNegativeOrZero(prop: String, value: Long, call: ValidationCall) {
        if (value > 0) {
            call.propertyError(prop, SingleConstraintError("NegativeOrZero"))
        }
    }

    fun checkNegativeOrZero(prop: String, value: Double, call: ValidationCall) {
        if (value > 0.0) {
            call.propertyError(prop, SingleConstraintError("NegativeOrZero"))
        }
    }

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
