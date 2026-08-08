package io.github.ktor_batterypack.validation.ksp

interface MemberFn {
    val name: String
    val paramName: String
    val paramType: CodegenType
    val returnType: CodegenType?
    val isPublic: Boolean
}
