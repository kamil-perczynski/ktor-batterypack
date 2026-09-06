package io.github.ktor_batterypack.validation.ksp

import io.github.ktor_batterypack.validation.codegen.ConstraintsDescriptor

data class CodegenModel(
    val imports: List<String>,
    val constraintImports: List<String>,
    val jsonConstraintImports: List<String>,
    val packageName: String,
    val interfaceName: String,
    val fqInterfaceName: String,
    val typeName: String,
    val fqTypeName: String,
    val methods: List<PublicValidationMethod>,
    val privateMethods: List<PrivateValidationMethod>,
    val methodsGraph: MethodsGraph
)

data class PublicValidationMethod(
    val name: String,
    val returnType: String,
    val param: ValidationParameter,
    val codegenType: CodegenType,
)

data class NestedProperty(
    val name: String,
    val nullable: Boolean,
    val codegenType: CodegenType,
    val type: String,
    val isList: Boolean,
    val isObject: Boolean,
    val constraints: Map<String, ConstraintMethod> = emptyMap(),
)

data class DirectProperties(
    val codegenType: CodegenType,
    val name: String,
    val type: String,
    val nullable: Boolean,
    val constraints: Map<String, ConstraintMethod>
)

data class PrivateValidationMethod(
    val name: String,
    val codegenType: CodegenType,
    val param: ValidationParameter,
    val directProperties: List<DirectProperties>,
    val nestedProperties: List<NestedProperty>,
    val additionalValidationMethods: List<CodegenMethod> = emptyList(),
    val isOverride: Boolean
)

data class ValidationParameter(
    val name: String,
    val codegenType: CodegenType,
    val type: String,
    val fqName: String,
    val isList: Boolean = false,
    val isObject: Boolean = false,
    val isMap: Boolean,
    val itemConstraints: Map<String, ConstraintMethod> = emptyMap(),
)

data class ConstraintMethod(
    val name: String,
    val args: Map<String, Any> = emptyMap(),
    val descriptor: ConstraintsDescriptor.Constraint
)
