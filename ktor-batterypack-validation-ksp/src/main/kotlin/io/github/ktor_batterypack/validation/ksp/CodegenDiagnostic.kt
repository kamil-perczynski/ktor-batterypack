package io.github.ktor_batterypack.validation.ksp

data class CodegenDiagnostic(
    val severity: Severity,
    val message: String,
    val symbol: Any?
) {
    enum class Severity {
        ERROR,
        WARNING
    }
}
