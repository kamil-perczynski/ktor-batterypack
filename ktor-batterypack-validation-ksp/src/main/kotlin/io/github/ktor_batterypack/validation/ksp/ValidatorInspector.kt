package io.github.ktor_batterypack.validation.ksp

import io.github.ktor_batterypack.validation.codegen.ConstraintRegistry
import io.github.ktor_batterypack.validation.ksp.CodegenDiagnostic.Severity
import io.github.ktor_batterypack.validation.ksp.DefaultCodegenNamingConvention.DEFAULT_CODEGEN_NAMING_CONVENTION

class ValidatorInspector(
    private val constraintRegistry: ConstraintRegistry = ConstraintRegistry.default(),
    private val namingConvention: CodegenNamingConvention = DEFAULT_CODEGEN_NAMING_CONVENTION,
    private val strict: Boolean = false
) {

    companion object {
        const val STRICT_OPTION = "ktor.validation.strict"

        private const val JSON_NODE_FQ_NAME = "tools.jackson.databind.JsonNode"
        private const val COLLECTION_TOKEN = "kotlin.collections.Collection"
        private const val MAP_TOKEN = "kotlin.collections.Map"

        private val CONSTRAINT_PACKAGE_PREFIXES = listOf("jakarta.validation.constraints.", "javax.validation.constraints.")
        private val CONSTRAINT_META_ANNOTATIONS = setOf("jakarta.validation.Constraint", "javax.validation.Constraint")
    }

    fun inspect(validatorInterface: ValidatorInterface, jsonMode: Boolean): List<CodegenDiagnostic> {
        val implementedOverrides = validatorInterface.memberFunctions
            .filter { function -> function.hasImplementation && MemberFnShape.of(function) == MemberFnShape.VALIDATE_CALL }
        val inspection = Inspection(jsonMode, implementedOverrides, CodegenTypeNames.of(validatorInterface))

        for (function in validatorInterface.memberFunctions) {
            inspectFunction(inspection, function)
        }

        return inspection.diagnostics
    }

    private fun inspectFunction(inspection: Inspection, function: MemberFn) {
        if (function.hasImplementation) {
            return
        }

        val shape = MemberFnShape.of(function)
        if (shape == MemberFnShape.BUILT_IN) {
            return
        }
        if (shape == MemberFnShape.UNSUPPORTED) {
            inspection.error(
                "Function `${function.name}` must be declared as `fun ${function.name}(value: T): ValidationResult<T>` " +
                    "or `fun validateT(value: T?, call: ValidationCall)`",
                function.symbol
            )
            return
        }

        val validationParamType = function.validationParamType
        if (inspection.jsonMode && validationParamType == null) {
            inspection.error(
                "Function `${function.name}` of a @JsonValidator must declare its model type with @ValidationParamType",
                function.symbol
            )
            return
        }

        val modelType = validationParamType ?: function.paramType
        if (!modelType.isResolved) {
            inspection.error("Function `${function.name}` validates unsupported type `$modelType`", function.symbol)
            return
        }

        if (shape == MemberFnShape.VALIDATE_RESULT) {
            inspectResultSignature(inspection, function, modelType)
        } else {
            inspectCallSignature(inspection, function, modelType)
        }

        inspectValue(inspection, modelType, emptyList(), "parameter of `${function.name}`", function.symbol)
    }

    private fun inspectResultSignature(inspection: Inspection, function: MemberFn, modelType: CodegenType) {
        val expectedFqName = if (inspection.jsonMode) JSON_NODE_FQ_NAME else modelType.fqName
        val expectedName = if (inspection.jsonMode) "JsonNode" else inspection.typeNames.sourceName(modelType)
        val resultType = function.returnType?.typeParams?.firstOrNull()

        if (function.paramType.fqName != expectedFqName || resultType?.fqName != expectedFqName) {
            inspection.error(
                "Function `${function.name}` must be declared as " +
                    "`fun ${function.name}(${function.paramName}: $expectedName): ValidationResult<$expectedName>`",
                function.symbol
            )
        }
    }

    private fun inspectCallSignature(inspection: Inspection, function: MemberFn, modelType: CodegenType) {
        val expectedName = namingConvention.validationMethodName(modelType, inspection.typeNames)
        if (function.name != expectedName) {
            inspection.error(
                "Function `${function.name}` overrides the validation of `${modelType.name}` and must be named `$expectedName`",
                function.symbol
            )
        }
    }

    private fun inspectValue(
        inspection: Inspection,
        type: CodegenType,
        annotations: List<CodegenAnnotation>,
        location: String,
        symbol: Any?
    ) {
        if (!type.isResolved) {
            inspection.error("${capitalize(location)} has unsupported type `$type`", symbol)
            return
        }

        inspectConstraints(inspection, type, annotations, location, symbol)

        if (type.isCollection || type.isMap) {
            val itemType = if (type.isMap) type.typeParams.getOrNull(1) else type.typeParams.getOrNull(0)
            if (itemType == null) {
                inspection.error("${capitalize(location)} has unsupported type `$type`", symbol)
                return
            }
            inspectValue(inspection, itemType, itemType.annotations, "items of $location", symbol)
            return
        }

        if (type.isEnum) {
            return
        }

        if (type.isPrimitive) {
            if (inspection.jsonMode && jsonScalarTypeCheckMethodName(type.fqName) == null) {
                inspection.error(
                    "${capitalize(location)} has type `${type.fqName}`, which a @JsonValidator cannot read from JSON",
                    symbol
                )
            }
            return
        }

        inspectObject(inspection, type)
    }

    private fun inspectObject(inspection: Inspection, type: CodegenType) {
        if (!inspection.visited.add(qualifiedTypeName(type))) {
            return
        }
        if (isImplementedByUser(inspection, type)) {
            return
        }

        for (property in type.declaredMemberProperties) {
            if (inspection.jsonMode && property.isJsonIgnored) {
                continue
            }
            inspectValue(
                inspection,
                property.type,
                property.annotations + property.type.annotations,
                "property `${type.name}.${property.name}`",
                property.symbol
            )
        }
    }

    private fun inspectConstraints(
        inspection: Inspection,
        type: CodegenType,
        annotations: List<CodegenAnnotation>,
        location: String,
        symbol: Any?
    ) {
        for (annotation in constraintRegistry.unwrapContainerAnnotations(annotations)) {
            val descriptor = constraintRegistry.findDescriptor(annotation.fqName)
            if (descriptor == null) {
                if (isConstraintAnnotation(annotation)) {
                    inspection.report(
                        if (strict) Severity.ERROR else Severity.WARNING,
                        "Constraint @${annotation.name} on $location is not supported and is ignored. " +
                            "Describe it in a constraints descriptor (ktor.validation.constraint.dir)",
                        symbol
                    )
                }
                continue
            }

            val supportedTypes = descriptor.supportedTypes ?: continue
            if (supportedTypes.none { supportedType -> isSupportedType(type, supportedType) }) {
                inspection.error(
                    "Constraint @${annotation.name} cannot be applied to $location of type `${type.fqName}`. " +
                        "Supported types: ${supportedTypes.joinToString(", ")}",
                    symbol
                )
            }
        }
    }

    private fun isImplementedByUser(inspection: Inspection, type: CodegenType): Boolean {
        val methodName = namingConvention.validationMethodName(type, inspection.typeNames)
        val typeKey = qualifiedTypeName(type)
        return inspection.implementedOverrides.any { function ->
            val paramType = function.validationParamType ?: function.paramType
            function.isPublic
                && function.name == methodName
                && qualifiedTypeName(paramType) == typeKey
        }
    }

    private fun isConstraintAnnotation(annotation: CodegenAnnotation): Boolean {
        if (CONSTRAINT_PACKAGE_PREFIXES.any { prefix -> annotation.fqName.startsWith(prefix) }) {
            return true
        }
        return annotation.metaAnnotations.any { metaAnnotation -> metaAnnotation in CONSTRAINT_META_ANNOTATIONS }
    }

    private fun isSupportedType(type: CodegenType, supportedType: String): Boolean {
        return when (supportedType) {
            COLLECTION_TOKEN -> type.isCollection
            MAP_TOKEN -> type.isMap
            else -> type.fqName == supportedType
        }
    }

    private fun capitalize(value: String): String {
        return value.replaceFirstChar { char -> char.uppercase() }
    }

    private class Inspection(
        val jsonMode: Boolean,
        val implementedOverrides: List<MemberFn>,
        val typeNames: CodegenTypeNames
    ) {
        val diagnostics = mutableListOf<CodegenDiagnostic>()
        val visited = mutableSetOf<String>()

        fun error(message: String, symbol: Any?) {
            report(Severity.ERROR, message, symbol)
        }

        fun report(severity: Severity, message: String, symbol: Any?) {
            diagnostics.add(CodegenDiagnostic(severity, message, symbol))
        }
    }
}
