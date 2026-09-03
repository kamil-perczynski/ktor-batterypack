package io.github.ktor_batterypack.validation

class ValidationException(val data: Any, val errors: ConstraintError, message: String? = null) :
    RuntimeException(message ?: "Validation failed")
