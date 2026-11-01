package io.github.ktor_batterypack.validation.codegen.constraint

import com.fasterxml.jackson.annotation.JsonProperty

data class ConstraintsDescriptor(
    @param:JsonProperty("defaults") val defaults: Defaults,
    @param:JsonProperty("constraints") val constraints: List<Constraint>
) {
    data class Defaults(
        @param:JsonProperty("className") val className: String
    )

    data class Constraint(
        @param:JsonProperty("annotation") val annotation: String,
        @param:JsonProperty("className") val className: String? = null,
        @param:JsonProperty("methodName") val methodName: String? = null,
        @param:JsonProperty("json") val json: JsonOverride? = null,
        @param:JsonProperty("rejectsNull") val rejectsNull: Boolean? = null,
        @param:JsonProperty("supportedTypes") val supportedTypes: List<String>? = null
    )

    data class JsonOverride(
        @param:JsonProperty("className") val className: String? = null,
        @param:JsonProperty("methodName") val methodName: String? = null
    )
}
