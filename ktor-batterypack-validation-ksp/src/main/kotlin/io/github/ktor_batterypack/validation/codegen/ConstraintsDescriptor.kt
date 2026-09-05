package io.github.ktor_batterypack.validation.codegen

import com.fasterxml.jackson.annotation.JsonProperty

data class ConstraintsDescriptor(
    @param:JsonProperty("import") val import: String,
    @param:JsonProperty("jsonImport") val jsonImport: String,
    @param:JsonProperty("defaults") val defaults: Defaults,
    @param:JsonProperty("constraints") val constraints: List<Constraint>
) {
    data class Defaults(
        @param:JsonProperty("callTpl") val callTpl: String,
        @param:JsonProperty("jsonCallTpl") val jsonCallTpl: String? = null
    )

    data class Constraint(
        @param:JsonProperty("annotation") val annotation: String,
        @param:JsonProperty("callTpl") val callTpl: String? = null,
        @param:JsonProperty("jsonCallTpl") val jsonCallTpl: String? = null
    )
}
