package io.github.ktor_batterypack.validation.ksp

interface MemberFn {
    val hasImplementation: Boolean
    val name: String
    val paramName: String
    val paramType: CodegenType
    val params: List<MemberFnParam>
    val returnType: CodegenType?
    val isPublic: Boolean
    val annotations: List<CodegenAnnotation>
    val validationParamType: CodegenType?
    val symbol: Any?
}

data class MemberFnParam(val name: String, val type: CodegenType)