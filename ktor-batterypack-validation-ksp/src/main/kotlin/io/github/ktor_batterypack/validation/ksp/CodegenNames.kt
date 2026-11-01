package io.github.ktor_batterypack.validation.ksp

import com.squareup.kotlinpoet.ClassName

class CodegenNames private constructor(private val methodNameSegments: Map<String, String>) {

    companion object {
        fun of(validatorInterface: ValidatorInterface): CodegenNames {
            val types = linkedMapOf<String, CodegenType>()
            for (function in validatorInterface.memberFunctions) {
                val shape = MemberFnShape.of(function)
                if (shape == MemberFnShape.VALIDATE_RESULT || shape == MemberFnShape.VALIDATE_CALL) {
                    collectReachableTypes(function.validationParamType ?: function.paramType, types)
                }
            }
            return CodegenNames(disambiguatedSegments(types.values))
        }

        private fun collectReachableTypes(type: CodegenType, types: MutableMap<String, CodegenType>) {
            if (!type.isResolved) {
                return
            }
            if (types.putIfAbsent(qualifiedTypeName(type), type) != null) {
                return
            }
            type.typeParams.forEach { typeParam -> collectReachableTypes(typeParam, types) }
            if (ValueKind.of(type) == ValueKind.OBJECT) {
                type.declaredMemberProperties.forEach { property -> collectReachableTypes(property.type, types) }
                PolymorphicTypes.directSubtypes(type).forEach { subtype -> collectReachableTypes(subtype.type, types) }
            }
        }

        private fun disambiguatedSegments(types: Collection<CodegenType>): Map<String, String> {
            return types
                .associate { type -> type.fqName to type.name }
                .entries
                .groupBy({ (_, simpleName) -> simpleName }, { (fqName, _) -> fqName })
                .filterValues { fqNames -> fqNames.size > 1 }
                .flatMap { (simpleName, fqNames) -> disambiguate(simpleName, fqNames).entries }
                .associate { (fqName, segment) -> fqName to segment }
        }

        private fun disambiguate(simpleName: String, fqNames: List<String>): Map<String, String> {
            val qualifiers = fqNames.associateWith { fqName ->
                fqName.removeSuffix(simpleName).removeSuffix(".").split('.').filter { segment -> segment.isNotEmpty() }
            }
            val maxDepth = qualifiers.values.maxOf { segments -> segments.size }
            for (depth in 1..maxDepth) {
                val segments = qualifiers.mapValues { (_, packageSegments) ->
                    packageSegments.takeLast(depth).joinToString("") { segment -> capitalize(segment) } + capitalize(simpleName)
                }
                if (segments.values.toSet().size == fqNames.size) {
                    return segments
                }
            }
            return fqNames.associateWith { fqName -> capitalize(fqName.replace('.', '_')) }
        }

        private fun capitalize(value: String): String {
            return value.replaceFirstChar { char -> char.uppercase() }
        }
    }

    fun implClassName(validatorInterface: ValidatorInterface): ClassName {
        return ClassName(validatorInterface.className.packageName, "${validatorInterface.name}Impl")
    }

    fun validationMethodName(type: CodegenType): String {
        return "validate${methodNameSegment(type)}"
    }

    fun collectionValidationMethodName(parentType: CodegenType, property: DeclaredMember): String {
        return "validate${methodNameSegment(parentType)}${capitalize(property.name)}${property.type.name}"
    }

    fun paramName(type: CodegenType): String {
        return type.name.replaceFirstChar { char -> char.lowercase() }
    }

    private fun methodNameSegment(type: CodegenType): String {
        val name = methodNameSegments[type.fqName] ?: capitalize(type.name)
        return name + type.typeParams.joinToString("") { typeParam -> methodNameSegment(typeParam) }
    }
}
