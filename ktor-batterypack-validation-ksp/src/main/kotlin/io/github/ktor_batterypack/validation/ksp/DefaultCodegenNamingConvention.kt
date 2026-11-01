package io.github.ktor_batterypack.validation.ksp

enum class DefaultCodegenNamingConvention : CodegenNamingConvention {

    DEFAULT_CODEGEN_NAMING_CONVENTION;

    override fun implTypeName(validatorInterface: ValidatorInterface): String {
        return "${validatorInterface.name}Impl"
    }

    override fun collectionValidationMethodName(
        parentType: CodegenType,
        property: DeclaredMember,
        typeNames: CodegenTypeNames
    ): String {
        val parentSegment = typeNames.methodNameSegment(parentType)
        return "validate$parentSegment${property.name.replaceFirstChar { it.uppercase() }}${property.type.name}"
    }

    override fun paramName(type: CodegenType): String {
        return type.name.replaceFirstChar { it.lowercase() }
    }

    override fun validationMethodName(type: CodegenType, typeNames: CodegenTypeNames): String {
        return "validate${typeNames.methodNameSegment(type)}"
    }
}
