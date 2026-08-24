package io.github.ktor_batterypack.validation

class ValidationException(
    val data: Any,
    val errors: ConstraintError
) : RuntimeException("Validation failed, errors=$errors")
