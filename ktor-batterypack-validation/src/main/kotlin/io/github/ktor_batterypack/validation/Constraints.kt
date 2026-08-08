package io.github.ktor_batterypack.validation

import java.time.LocalDate

object Constraints {

    fun checkNotNull(prop: String, data: Any?, call: ValidationCall) {
        if (data == null) {
            call.propertyError(prop, SingleConstraintError("NotNull"))
        }
    }

    fun checkNotEmpty(prop: String, data: Collection<Any>, call: ValidationCall) {
        if (data.isEmpty()) {
            if (prop.isEmpty()) {
                call.directError(SingleConstraintError("NotEmpty"))
            } else {
                call.propertyError(prop, SingleConstraintError("NotEmpty"))
            }
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

    fun checkMin(prop: String, num: Double, call: ValidationCall, value: Number) {
        if (num < value.toDouble()) {
            call.propertyError(prop, SingleConstraintError("Min", "Must be at least $value"))
        }
    }

    fun checkMax(prop: String, num: Double, call: ValidationCall, value: Number) {
        if (num > value.toDouble()) {
            call.propertyError(prop, SingleConstraintError("Max", "Must be at most $value"))
        }
    }

    fun checkMin(prop: String, num: Int, call: ValidationCall, value: Number) {
        if (num < value.toDouble()) {
            call.propertyError(prop, SingleConstraintError("Min", "Must be at least $value"))
        }
    }

    fun checkMax(prop: String, num: Int, call: ValidationCall, value: Number) {
        if (num > value.toDouble()) {
            call.propertyError(prop, SingleConstraintError("Max", "Must be at most $value"))
        }
    }

    fun checkPattern(prop: String, str: String, call: ValidationCall, regexp: String) {
        if (!Regex(regexp).matches(str)) {
            call.propertyError(prop, SingleConstraintError("Pattern", "Must match $regexp"))
        }
    }

}