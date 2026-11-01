package io.github.ktor_batterypack.validation.ksp

import com.squareup.kotlinpoet.ClassName

data class ValidatorModel(
    val implClassName: ClassName,
    val interfaceClassName: ClassName,
    val mode: CodegenMode,
    val entryMethods: List<EntryMethod>,
    val typeMethods: List<TypeMethod>
)
