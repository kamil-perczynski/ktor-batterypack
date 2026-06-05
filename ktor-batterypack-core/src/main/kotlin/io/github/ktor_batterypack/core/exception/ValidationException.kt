package io.github.ktor_batterypack.core.exception

import com.fasterxml.jackson.annotation.JsonAnyGetter

/**
 * Thrown when request validation fails, carrying a list of field-level errors.
 */
class ValidationException(val errors: List<FieldError>) : RuntimeException()

/**
 * Describes a single validation error for a specific field.
 */
data class FieldError(
    val field: String,
    val message: String,
    @get:JsonAnyGetter
    val extras: Map<String, Any> = emptyMap()
)
