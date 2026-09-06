package io.github.ktor_batterypack.validation.codegen.helper

internal const val DEFAULT_CONSTRAINT_TEMPLATE =
    "{{simpleClassName}}.{{methodName}}({{quote prop}}, {{value}}, {{call}}{{#with (constraintArgs args)}}, {{this}}{{/with}})"

data class ConstraintTemplateContext(
    val simpleClassName: String,
    val methodName: String,
    val prop: String,
    val value: String,
    val call: String,
    val args: Map<String, Any>
)
