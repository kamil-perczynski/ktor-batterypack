package io.github.ktor_batterypack.validation.ksp

enum class DefaultCodegenNamingConvention : CodegenNamingConvention {

    DEFAULT_CODEGEN_NAMING_CONVENTION;

    override fun ifaceTypeName(ifaceType: ValidatorInterface): String {
        return "${ifaceType.name}Impl"
    }

    override fun ifaceFqTypeName(ifaceType: ValidatorInterface): String {
        return "${ifaceType.fqName}Impl"
    }

    override fun collectionValidationMethodName(
        parentType: CodegenType,
        property: DeclaredMember,
        typeNames: CodegenTypeNames
    ): String {
        return collectionValidationMethodName(parentType, property.name, property.type, typeNames)
    }

    override fun collectionValidationMethodName(
        parentType: CodegenType,
        propertyName: String,
        propertyType: CodegenType,
        typeNames: CodegenTypeNames
    ): String {
        val parentSegment = typeNames.methodNameSegment(parentType)
        return "validate$parentSegment${propertyName.replaceFirstChar { it.uppercase() }}${propertyType.name}"
    }

    override fun paramName(type: CodegenType): String {
        return type.name.replaceFirstChar { it.lowercase() }
    }

    override fun validationMethodName(type: CodegenType, typeNames: CodegenTypeNames): String {
        return "validate${typeNames.methodNameSegment(type)}"
    }
}
