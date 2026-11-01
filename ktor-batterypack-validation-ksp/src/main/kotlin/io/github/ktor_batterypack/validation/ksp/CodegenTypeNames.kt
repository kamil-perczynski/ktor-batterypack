package io.github.ktor_batterypack.validation.ksp

class CodegenTypeNames(types: Collection<CodegenType>, reservedImports: Collection<String> = emptyList()) {

    companion object {
        fun of(validatorInterface: ValidatorInterface, reservedImports: Collection<String> = emptyList()): CodegenTypeNames {
            val types = linkedMapOf<String, CodegenType>()
            for (function in validatorInterface.memberFunctions) {
                val shape = MemberFnShape.of(function)
                if (shape == MemberFnShape.VALIDATE_RESULT || shape == MemberFnShape.VALIDATE_CALL) {
                    collectReachableTypes(function.validationParamType ?: function.paramType, types)
                }
            }
            return CodegenTypeNames(types.values, reservedImports + validatorInterface.fqName)
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
    private val qualifiedFqNames: Set<String>

    init {
        val simpleNames = types.associate { type -> type.fqName to type.name }
        val collidingGroups = simpleNames.entries
            .groupBy({ (_, simpleName) -> simpleName }, { (fqName, _) -> fqName })
            .filterValues { fqNames -> fqNames.size > 1 }

        methodNameSegments = collidingGroups
            .flatMap { (simpleName, fqNames) -> disambiguate(simpleName, fqNames).entries }
            .associate { (fqName, segment) -> fqName to segment }

        val reservedBySimpleName = reservedImports.groupBy { reservedImport -> reservedImport.substringAfterLast('.') }
        val reservedCollisions = simpleNames
            .filter { (fqName, simpleName) -> reservedBySimpleName[simpleName].orEmpty().any { reserved -> reserved != fqName } }
            .keys
        qualifiedFqNames = collidingGroups.values.flatten().toSet() + reservedCollisions
    }

    fun sourceName(type: CodegenType): String {
        val name = if (type.fqName in qualifiedFqNames) type.fqName else type.name
        if (type.typeParams.isEmpty()) {
            return name
        }
        val typeArgs = type.typeParams.joinToString(", ") { typeParam ->
            if (typeParam.isMarkedNullable) sourceName(typeParam) + '?' else sourceName(typeParam)
        }
        return "$name<$typeArgs>"
    }

    fun methodNameSegment(type: CodegenType): String {
        val name = methodNameSegments[type.fqName] ?: capitalize(type.name)
        return name + type.typeParams.joinToString("") { typeParam -> methodNameSegment(typeParam) }
    }

    fun imports(type: CodegenType): List<String> {
        val typeParamImports = type.typeParams.flatMap { typeParam -> imports(typeParam) }
        if (type.fqName in qualifiedFqNames) {
            return typeParamImports
        }
        return listOf(type.fqName) + typeParamImports
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
