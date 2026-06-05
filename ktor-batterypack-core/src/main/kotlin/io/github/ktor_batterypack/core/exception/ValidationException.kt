package io.github.ktor_batterypack.core.exception

import com.fasterxml.jackson.annotation.JsonAnyGetter

class ValidationException(val errors: List<FieldError>) : RuntimeException()

data class FieldError(
    val field: String,
    val message: String,
    @get:JsonAnyGetter
    val extras: Map<String, Any> = emptyMap()
)
