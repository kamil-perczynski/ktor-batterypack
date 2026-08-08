package io.github.ktor_batterypack.validation

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ObjectConstraintError(
    @get:JsonAnyGetter
    val properties: Map<String, ConstraintError>
) : ConstraintError