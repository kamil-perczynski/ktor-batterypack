package io.github.ktor_batterypack.validation.ksp

data class CodegenMethod(
    val name: String,
    val paramName: String,
    val param: CodegenType,
    val isPublic: Boolean,
    val hasImplementation: Boolean,
    val returnType: CodegenType? = null,
    val annotations: List<CodegenAnnotation> = emptyList(),
    val isOverride: Boolean,
) : CodegenNode
