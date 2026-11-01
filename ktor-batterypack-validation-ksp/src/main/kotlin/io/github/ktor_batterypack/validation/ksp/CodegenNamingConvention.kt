package io.github.ktor_batterypack.validation.ksp

interface CodegenNamingConvention {

    /** Simple name of the generated validator implementation class, e.g. `PersonValidatorImpl`. */
    fun ifaceTypeName(ifaceType: ValidatorInterface): String

    /** Fully qualified name of the generated validator implementation class, e.g. `com.example.PersonValidatorImpl`. */
    fun ifaceFqTypeName(ifaceType: ValidatorInterface): String

    fun validationMethodName(type: CodegenType, typeNames: CodegenTypeNames): String

    fun paramName(type: CodegenType): String

    fun collectionValidationMethodName(
        parentType: CodegenType,
        property: DeclaredMember,
        typeNames: CodegenTypeNames
    ): String

    fun collectionValidationMethodName(
        parentType: CodegenType,
        propertyName: String,
        propertyType: CodegenType,
        typeNames: CodegenTypeNames
    ): String
}
