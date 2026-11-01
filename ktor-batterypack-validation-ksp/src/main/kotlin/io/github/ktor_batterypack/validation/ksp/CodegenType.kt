package io.github.ktor_batterypack.validation.ksp

import com.squareup.kotlinpoet.TypeName

interface CodegenType {
    val name: String
    val fqName: String
    val typeName: TypeName
    val isResolved: Boolean
    val isError: Boolean
    val typeParams: List<CodegenType>
    val isCollection: Boolean
    val isMap: Boolean
    val isEnum: Boolean
    val isSealed: Boolean
    val isAbstract: Boolean
    val declaredMemberProperties: List<DeclaredMember>
    val annotations: List<CodegenAnnotation>
    val declarationAnnotations: List<CodegenAnnotation>
    val supertypes: List<CodegenType>
    val sealedSubtypes: List<CodegenType>
    val jsonSubtypes: List<CodegenSubtype>
    val isMarkedNullable: Boolean
    val enumValues: List<String>
}
