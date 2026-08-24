package io.github.ktor_batterypack.validation.ksp

interface CodegenType : CodegenNode {
    val name: String
    val fqName: String
    val typeParams: List<CodegenType>
    val isPrimitive: Boolean
    val isCollection: Boolean
    val isMap: Boolean
    val isEnum: Boolean
    val declaredMemberProperties: List<DeclaredMember>
    val properName: String
    val annotations: List<CodegenAnnotation>
    val isMarkedNullable: Boolean
}