package io.github.ktor_batterypack.validation.ksp

interface CodegenNamingConvention {

    fun implTypeName(validatorInterface: ValidatorInterface): String

    fun validationMethodName(type: CodegenType, typeNames: CodegenTypeNames): String

    fun paramName(type: CodegenType): String

    fun collectionValidationMethodName(
        parentType: CodegenType,
        property: DeclaredMember,
        typeNames: CodegenTypeNames
    ): String
}
