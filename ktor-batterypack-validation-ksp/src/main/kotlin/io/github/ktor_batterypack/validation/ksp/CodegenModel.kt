package io.github.ktor_batterypack.validation.ksp

data class CodegenModel(
    val imports: List<String>,
    val packageName: String,
    val interfaceName: String,
    val fqInterfaceName: String,
    val typeName: String,
    val fqTypeName: String,
    val methods: List<PublicValidationMethod>,
    val privateMethods: List<PrivateValidationMethod>
)

data class PublicValidationMethod(
    val name: String,
    val returnType: String,
    val param: ValidationParameter,
    val validationMethodName: String
)

data class NestedProperty(
    val name: String,
    val nullable: Boolean,
    val type: String,
    val isList: Boolean,
    val isObject: Boolean,
    val constraints: Map<String, ConstraintMethod> = emptyMap(),
    val _itemValidatorMethodName: String? = null
) {
    val validatorMethodName: String
        get() = _itemValidatorMethodName ?: "validate${name.replaceFirstChar { it.uppercase() }}"
}

data class DirectProperties(
    val name: String,
    val type: String,
    val nullable: Boolean,
    val constraints: Map<String, ConstraintMethod>
)

data class PrivateValidationMethod(
    val name: String,
    val param: ValidationParameter,
    val directProperties: List<DirectProperties>,
    val nestedProperties: List<NestedProperty>
)

data class ValidationParameter(
    val name: String,
    val type: String,
    val fqName: String,
    val isList: Boolean = false,
    val isObject: Boolean = false,
    private val _itemValidatorMethodName: String? = null
) {
    val itemValidatorMethodName: String?
        get() = _itemValidatorMethodName ?: if (isObject) null else "validate${name.replaceFirstChar { it.uppercase() }}"
}

data class ConstraintMethod(
    val name: String,
    val args: Map<String, Any> = emptyMap()
)
