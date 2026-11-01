package io.github.ktor_batterypack.validation.ksp

data class DispatchBranch(
    val type: CodegenType,
    val typeIds: List<String>,
    val validateWith: String?
)
