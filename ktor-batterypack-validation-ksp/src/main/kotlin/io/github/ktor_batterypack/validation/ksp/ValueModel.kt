package io.github.ktor_batterypack.validation.ksp

import io.github.ktor_batterypack.validation.codegen.ConstraintCall

data class ValueModel(
    val type: CodegenType,
    val kind: ValueKind,
    val checkNotNull: Boolean,
    val constraints: List<ConstraintCall>,
    val validateWith: String?
)
