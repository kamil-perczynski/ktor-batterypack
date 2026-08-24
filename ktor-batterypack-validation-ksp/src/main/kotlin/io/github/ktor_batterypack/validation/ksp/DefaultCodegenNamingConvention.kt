package io.github.ktor_batterypack.validation.ksp

enum class DefaultCodegenNamingConvention : CodegenNamingConvention {

    DEFAULT_CODEGEN_NAMING_CONVENTION;

    override fun ifaceTypeName(ifaceType: ValidatorInterface): String {
        return "${ifaceType.name}Impl"
    }

    override fun ifaceFqTypeName(ifaceType: ValidatorInterface): String {
        return "${ifaceType.fqName}Impl"
    }

    override fun itemValidatorMethodName(type: CodegenType): String? {
        return when {
            type.isCollection ->
                if (!type.typeParams.first().isPrimitive)
                    validationMethodName(type.typeParams.first())
                else ""

            type.isMap ->
                if (!type.typeParams[1].isPrimitive)
                    validationMethodName(type.typeParams[1])
                else ""

            else -> null
        }
    }

    override fun collectionValidationMethodName(parentType: CodegenType, property: DeclaredMember): String {
        return "validate${parentType.name}${property.name.replaceFirstChar { it.uppercase() }}${property.type.name}"
    }

    override fun collectionValidationMethodName(parentType: CodegenType, propertyName: String, propertyType: CodegenType): String {
        return "validate${parentType.name}${propertyName.replaceFirstChar { it.uppercase() }}${propertyType.name}"
    }

    override fun paramName(type: CodegenType): String {
        return type.name.replaceFirstChar { it.lowercase() }
    }

    override fun validationMethodName(type: CodegenType): String {
        return "validate${type.name.replaceFirstChar { it.uppercase() }}"
    }
}
