package io.github.ktor_batterypack.validation.codegen

import com.fasterxml.jackson.annotation.JsonProperty

data class ConstraintsDescriptor(
    @param:JsonProperty("imports") val imports: List<String>,
    @param:JsonProperty("jsonImports") val jsonImports: List<String>,
    @param:JsonProperty("defaults") val defaults: Defaults,
    @param:JsonProperty("constraints") val constraints: List<Constraint>
) {
    data class Defaults(
        @param:JsonProperty("simpleClassName") val simpleClassName: String,
        @param:JsonProperty("callTpl") val callTpl: String? = null,
        @param:JsonProperty("jsonCallTpl") val jsonCallTpl: String? = null
    )

    data class Constraint(
        @param:JsonProperty("annotation") val annotation: String,
        @param:JsonProperty("simpleClassName") val simpleClassName: String? = null,
        @param:JsonProperty("callTpl") val callTpl: String? = null,
        @param:JsonProperty("jsonCallTpl") val jsonCallTpl: String? = null
    )
}
