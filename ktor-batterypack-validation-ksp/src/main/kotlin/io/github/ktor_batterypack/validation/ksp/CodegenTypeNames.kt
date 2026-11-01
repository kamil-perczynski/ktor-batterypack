package io.github.ktor_batterypack.validation.ksp

class CodegenTypeNames(types: Collection<CodegenType>) {

    companion object {
        fun of(validatorInterface: ValidatorInterface): CodegenTypeNames {
            val types = linkedMapOf<String, CodegenType>()
            for (function in validatorInterface.memberFunctions) {
                val shape = MemberFnShape.of(function)
                if (shape == MemberFnShape.VALIDATE_RESULT || shape == MemberFnShape.VALIDATE_CALL) {
                    collectReachableTypes(function.validationParamType ?: function.paramType, types)
                }
            }
            return CodegenTypeNames(types.values)
        }

        private fun collectReachableTypes(type: CodegenType, types: MutableMap<String, CodegenType>) {
            if (!type.isResolved) {
                return
            }
            if (types.putIfAbsent(qualifiedTypeName(type), type) != null) {
                return
            }
            type.typeParams.forEach { typeParam -> collectReachableTypes(typeParam, types) }
            type.declaredMemberProperties.forEach { property -> collectReachableTypes(property.type, types) }
        }
    }

    private val methodNameSegments: Map<String, String>

    init {
        methodNameSegments = types
            .associate { type -> type.fqName to type.name }
            .entries
            .groupBy({ (_, simpleName) -> simpleName }, { (fqName, _) -> fqName })
            .filterValues { fqNames -> fqNames.size > 1 }
            .flatMap { (simpleName, fqNames) -> disambiguate(simpleName, fqNames).entries }
            .associate { (fqName, segment) -> fqName to segment }
    }

    fun methodNameSegment(type: CodegenType): String {
        val name = methodNameSegments[type.fqName] ?: capitalize(type.name)
        return name + type.typeParams.joinToString("") { typeParam -> methodNameSegment(typeParam) }
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
