package io.github.ktor_batterypack.validation

class ValidationException(
    val data: Any,
    val validatorClass: Class<*>,
    val errors: ConstraintError
) : RuntimeException("Validation failed")
