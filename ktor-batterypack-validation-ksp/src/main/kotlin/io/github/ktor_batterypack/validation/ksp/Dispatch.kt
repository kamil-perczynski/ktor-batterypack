package io.github.ktor_batterypack.validation.ksp

data class Dispatch(
    val typeIdProperty: String?,
    val branches: List<DispatchBranch>,
    val exhaustive: Boolean
)
