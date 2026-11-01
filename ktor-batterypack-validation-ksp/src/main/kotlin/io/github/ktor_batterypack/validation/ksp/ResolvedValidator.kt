package io.github.ktor_batterypack.validation.ksp

data class ResolvedValidator(
    val model: ValidatorModel?,
    val diagnostics: List<CodegenDiagnostic>,
    val hasUnresolvedTypes: Boolean = false
)
