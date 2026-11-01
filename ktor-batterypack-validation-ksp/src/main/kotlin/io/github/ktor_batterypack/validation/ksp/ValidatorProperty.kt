package io.github.ktor_batterypack.validation.ksp

data class ValidatorProperty(
    val name: String,
    val type: CodegenType,
    val isMutable: Boolean,
    val symbol: Any?
)
