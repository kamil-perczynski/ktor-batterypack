package io.github.ktor_batterypack.validation.ksp

interface CodegenNamingConvention {

    /** Simple name of the generated validator implementation class, e.g. `PersonValidatorImpl`. */
    fun ifaceTypeName(ifaceType: ValidatorInterface): String

    /** Fully qualified name of the generated validator implementation class, e.g. `com.example.PersonValidatorImpl`. */
    fun ifaceFqTypeName(ifaceType: ValidatorInterface): String

    /** Name of the validator method for items of a collection/map parameter, or `null` for non-container types. */
    fun itemValidatorMethodName(type: CodegenType): String?

    fun validationMethodName(type: CodegenType): String

    fun paramName(type: CodegenType): String

    fun collectionValidationMethodName(parentType: CodegenType, property: DeclaredMember): String
    fun collectionValidationMethodName(
        parentType: CodegenType,
        propertyName: String,
        propertyType: CodegenType
    ): String
}
