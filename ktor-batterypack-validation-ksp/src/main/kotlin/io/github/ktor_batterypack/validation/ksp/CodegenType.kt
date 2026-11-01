package io.github.ktor_batterypack.validation.ksp

import com.squareup.kotlinpoet.TypeName

interface CodegenType {
    val name: String
    val fqName: String
    val typeName: TypeName
    val isResolved: Boolean
    val typeParams: List<CodegenType>
    val isPrimitive: Boolean
    val isCollection: Boolean
    val isMap: Boolean
    val isEnum: Boolean
    val declaredMemberProperties: List<DeclaredMember>
    val annotations: List<CodegenAnnotation>
    val isMarkedNullable: Boolean
    val enumValues: List<String>
}
