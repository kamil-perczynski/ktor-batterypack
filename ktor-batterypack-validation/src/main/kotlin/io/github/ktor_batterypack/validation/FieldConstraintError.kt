package io.github.ktor_batterypack.validation

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonValue

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FieldConstraintError(
    @get:JsonValue
    val errors: List<SingleConstraintError>
) : ConstraintError

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SingleConstraintError(
    val constraint: String,
    val message: String? = null,
)
