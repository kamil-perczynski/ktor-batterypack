package io.github.ktor_batterypack.validation

import java.math.BigDecimal
import java.time.LocalDate

object Constraints {

    fun checkNotNull(prop: String, data: Any?, call: ValidationCall) {
        if (data == null) {
            call.propertyError(prop, SingleConstraintError("NotNull"))
        }
    }

    fun checkNotEmpty(prop: String, data: Collection<Any>, call: ValidationCall) {
        if (data.isEmpty()) {
            call.directError(SingleConstraintError("NotEmpty"))
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

    fun checkPast(prop: String, date: LocalDate, call: ValidationCall) {
        if (!date.isBefore(LocalDate.now())) {
            call.propertyError(prop, SingleConstraintError("Past"))
        }
    }

    fun checkPattern(prop: String, str: String, call: ValidationCall, regexp: String) {
        if (!Regex(regexp).matches(str)) {
            call.propertyError(prop, SingleConstraintError("Pattern", "Must match $regexp"))
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










    // ==========================================
    // Int Overloads (Zero Boxing, Inlined)
    // ==========================================

    fun checkMin(prop: String, num: Int, call: ValidationCall, value: Int) {
        if (num < value) call.propertyError(prop, SingleConstraintError("Min", "Must be at least $value"))
    }

    fun checkMax(prop: String, num: Int, call: ValidationCall, value: Int) {
        if (num > value) call.propertyError(prop, SingleConstraintError("Max", "Must be at most $value"))
    }

    // ==========================================
    // Long Overloads (Zero Boxing, Inlined)
    // ==========================================

    fun checkMin(prop: String, num: Long, call: ValidationCall, value: Long) {
        if (num < value) call.propertyError(prop, SingleConstraintError("Min", "Must be at least $value"))
    }

    fun checkMax(prop: String, num: Long, call: ValidationCall, value: Long) {
        if (num > value) call.propertyError(prop, SingleConstraintError("Max", "Must be at most $value"))
    }

    fun checkMin(prop: String, num: Double, call: ValidationCall, value: Long) {
        if (num < value) call.propertyError(prop, SingleConstraintError("Min", "Must be at least $value"))
    }

    fun checkMax(prop: String, num: Double, call: ValidationCall, value: Long) {
        if (num > value) call.propertyError(prop, SingleConstraintError("Max", "Must be at most $value"))
    }

    fun checkPositive(prop: String, value: BigDecimal, call: ValidationCall) {
        if (value <= BigDecimal.ZERO) {
            call.propertyError(prop, SingleConstraintError("Positive"))
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

}