package io.github.ktor_batterypack.validation

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ListConstraintError(
    val errors: List<SingleConstraintError>,
    val items: List<ConstraintError?>,
) : ConstraintError