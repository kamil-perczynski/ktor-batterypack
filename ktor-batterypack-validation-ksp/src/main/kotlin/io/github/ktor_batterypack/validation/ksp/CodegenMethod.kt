package io.github.ktor_batterypack.validation.ksp

data class CodegenMethod(
    val name: String,
    val paramName: String,
    val param: CodegenType,
    val isPublic: Boolean,
    val returnType: CodegenType? = null,
) : CodegenNode