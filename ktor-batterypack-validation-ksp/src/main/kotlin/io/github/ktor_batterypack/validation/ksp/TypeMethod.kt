package io.github.ktor_batterypack.validation.ksp

import com.squareup.kotlinpoet.TypeName

data class TypeMethod(
    val name: String,
    val paramName: String,
    val type: CodegenType,
    val kind: ValueKind,
    val declaredParamType: TypeName?,
    val properties: List<PropertyModel>,
    val item: ValueModel?,
    val dispatch: Dispatch?,
    val additionalCalls: List<String>,
    val isSuspend: Boolean
)
