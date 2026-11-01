package io.github.ktor_batterypack.validation.ksp

import com.google.common.graph.GraphBuilder
import com.google.common.graph.MutableGraph
import com.google.common.graph.Traverser
import io.github.ktor_batterypack.validation.codegen.ConstraintRegistry
import io.github.ktor_batterypack.validation.ksp.DefaultCodegenNamingConvention.DEFAULT_CODEGEN_NAMING_CONVENTION

private const val MAX_TYPE_GRAPH_PASSES = 1000

private val JSON_NODE_PACKAGE_PREFIXES = listOf("tools.jackson.databind.", "com.fasterxml.jackson.databind.")

private val TEMPLATE_IMPORTS = listOf(
    "io.github.ktor_batterypack.validation.ValidationResult",
    "io.github.ktor_batterypack.validation.SingleConstraintError",
    "io.github.ktor_batterypack.validation.ValidationCall",
    "javax.annotation.processing.Generated",
)

class CodegenModelResolver(
    private val namingConvention: CodegenNamingConvention = DEFAULT_CODEGEN_NAMING_CONVENTION,
    private val constraintRegistry: ConstraintRegistry = ConstraintRegistry.default(),
    private val jsonNamingStrategy: JsonNamingStrategy = JsonNamingStrategy.IDENTITY
) {

    fun resolve(validatorInterface: ValidatorInterface): CodegenModel {
        val graph = GraphBuilder.directed().build<CodegenNode>()
        val typeNames = CodegenTypeNames.of(validatorInterface, TEMPLATE_IMPORTS + constraintRegistry.constraintImports())

        val topLevelMethods = findTopLevelMethods(validatorInterface, graph)
        populateTypesGraph(graph, typeNames)

        val imports = mutableSetOf("io.github.ktor_batterypack.validation.ValidationResult")
        val privateMethods = mutableListOf<PrivateValidationMethod>()
        val publicMethods = mutableListOf<PublicValidationMethod>()

        Traverser.forGraph(graph).breadthFirst(topLevelMethods).forEach { node ->
            when (node) {
                is CodegenMethod -> {
                    imports.addAll(typeNames.imports(node.param))

                    if (node.isPublic && !node.hasImplementation && node.returnType?.fqName == "io.github.ktor_batterypack.validation.ValidationResult") {
                        publicMethods.add(toPublicMethod(node, typeNames))
                    } else if (!node.isPublic) {
                        val additionalValidationMethods = graph.nodes()
                            .asSequence()
                            .filterIsInstance<CodegenMethod>()
                            .filter { it.isPublic && it.hasImplementation }
                            .filter { qualifiedTypeName(it.param) == qualifiedTypeName(node.param) }
                            .filter { it.name != node.name }
                            .toList()

                        val privateMethod = toPrivateMethod(node, additionalValidationMethods, typeNames)
                        privateMethods.add(privateMethod)
                    }
                }

                is CodegenType -> throw IllegalStateException("All types should be resolved. Offender: $node")
            }
        }

        return CodegenModel(
            imports = imports.sorted(),
            constraintImports = constraintRegistry.constraintImports(),
            jsonConstraintImports = constraintRegistry.jsonConstraintImports(),
            packageName = validatorInterface.fqName.substringBeforeLast("."),
            interfaceName = validatorInterface.name,
            fqInterfaceName = validatorInterface.fqName,
            typeName = namingConvention.ifaceTypeName(validatorInterface),
            fqTypeName = namingConvention.ifaceFqTypeName(validatorInterface),
            methods = publicMethods.sortedBy { it.name },
            privateMethods = privateMethods.sortedBy { it.name },
            methodsGraph = GuavaMethodsGraph(graph),
            typeNames = typeNames
        )
    }

    private fun findTopLevelMethods(
        validatorInterface: ValidatorInterface,
        graph: MutableGraph<CodegenNode>
    ): List<CodegenMethod> {
        val topLevelMethods = mutableListOf<CodegenMethod>()

        for (function in validatorInterface.memberFunctions) {
            if (!isGraphedFunction(function)) {
                continue
            }

            val method = CodegenMethod(
                name = function.name,
                paramName = function.paramName,
                param = function.validationParamType ?: function.paramType,
                returnType = function.returnType,
                isPublic = function.isPublic,
                hasImplementation = function.hasImplementation,
                annotations = function.annotations,
                isOverride = true
            )

            graph.addNode(method)

            // existing methods that already have implementations
            // should not be inferred deeper
            if (!method.hasImplementation) {
                topLevelMethods.add(method)
            }

            if (!method.hasImplementation) {
                graph.putEdge(method, function.validationParamType ?: function.paramType)
            } else {
                graph.putEdge(function.validationParamType ?: function.paramType, method)
            }
        }

        return topLevelMethods
    }

    private fun populateTypesGraph(graph: MutableGraph<CodegenNode>, typeNames: CodegenTypeNames) {
        var passes = 0
        while (true) {
            val types = graph.nodes().filterIsInstance<CodegenType>()
            if (types.isEmpty()) {
                return
            }
            check(++passes <= MAX_TYPE_GRAPH_PASSES) {
                "Validation model did not converge after $MAX_TYPE_GRAPH_PASSES passes. Pending types: $types"
            }
            for (type in types) {
                val properties = type.declaredMemberProperties
                val methods = graph.predecessors(type)
                val methodName = namingConvention.validationMethodName(type, typeNames)
                val typeKey = qualifiedTypeName(type)

                val matchingOverride = graph.nodes()
                    .filterIsInstance<CodegenMethod>()
                    .find { method ->
                        method.isPublic
                            && method.name == methodName
                            && qualifiedTypeName(method.param) == typeKey
                            && method.returnType?.fqName == "kotlin.Unit"
                    }

                val codegenMethod = CodegenMethod(
                    name = methodName,
                    paramName = namingConvention.paramName(type),
                    param = type,
                    isPublic = false,
                    hasImplementation = false,
                    isOverride = matchingOverride != null
                )

                val matchingDuplicate = graph.nodes()
                    .filterIsInstance<CodegenMethod>()
                    .find { method ->
                        method.isPublic == codegenMethod.isPublic
                            && method.name == codegenMethod.name
                            && method.isOverride == codegenMethod.isOverride
                            && qualifiedTypeName(method.param) == typeKey
                            && method.returnType?.fqName == codegenMethod.returnType?.fqName
                    }

                if (matchingDuplicate != null) {
                    graph.removeNode(type)
                    continue
                }

                if (matchingOverride == null || !matchingOverride.hasImplementation) {
                    for (property in properties) {
                        if (property.type.isCollection || property.type.isMap) {
                            val collectionMethod = CodegenMethod(
                                name =
                                    namingConvention.collectionValidationMethodName(type, property, typeNames),
                                paramName = property.name,
                                param = property.type,
                                isPublic = false,
                                hasImplementation = false,
                                isOverride = false
                            )
                            graph.putEdge(codegenMethod, collectionMethod)

                            for (typeParam in property.type.typeParams) {
                                if (!typeParam.isPrimitive) {
                                    graph.putEdge(collectionMethod, typeParam)
                                }
                            }
                        } else {
                            if (!property.type.isPrimitive) {
                                graph.putEdge(codegenMethod, property.type)
                            }
                        }
                    }

                    methods.forEach { method ->
                        graph.putEdge(method, codegenMethod)
                    }
                }

                graph.removeNode(type)
            }
        }
    }

    private fun toPublicMethod(method: CodegenMethod, typeNames: CodegenTypeNames): PublicValidationMethod {
        return PublicValidationMethod(
            name = method.name,
            returnType = typeNames.sourceName(method.returnType!!),
            codegenType = method.param,
            param = ValidationParameter(
                name = method.paramName,
                codegenType = method.param,
                type = typeNames.sourceName(method.param),
                fqName = method.param.fqName,
                isList = method.param.isCollection,
                isMap = method.param.isMap,
                isObject = !method.param.isPrimitive && !method.param.isCollection && !method.param.isEnum,
            )
        )
    }

    private fun toPrivateMethod(
        method: CodegenMethod,
        additionalValidationMethods: List<CodegenMethod>,
        typeNames: CodegenTypeNames
    ): PrivateValidationMethod {
        val itemConstraints = constraintRegistry.findItemConstraints(method.param)
        val itemType = findItemType(method.param)

        return PrivateValidationMethod(
            name = method.name,
            codegenType = method.param,
            isOverride = method.isOverride,
            additionalValidationMethods = additionalValidationMethods,
            param = ValidationParameter(
                name = method.paramName,
                codegenType = method.param,
                type = typeNames.sourceName(method.param),
                fqName = method.param.fqName,
                isList = method.param.isCollection,
                isObject = isObject(method.param),
                isMap = method.param.isMap,
                itemConstraints = itemConstraints,
                itemNullable = itemType?.isMarkedNullable ?: false,
                itemRequired = itemType != null && (!itemType.isMarkedNullable || rejectsNull(itemConstraints)),
            ),
            directProperties = method.param.declaredMemberProperties
                .filter { it.type.isPrimitive }
                .map {
                    val constraints = constraintRegistry.toConstraints(it.type, it.annotations + it.type.annotations)
                    DirectProperties(
                        name = it.name,
                        jsonName = jsonNamingStrategy.translate(it),
                        codegenType = it.type,
                        type = typeNames.sourceName(it.type),
                        nullable = it.type.isMarkedNullable,
                        required = isRequired(it, constraints),
                        jsonIgnored = it.isJsonIgnored,
                        constraints = constraints,
                    )
                },
            nestedProperties = method.param.declaredMemberProperties
                .filter { !it.type.isPrimitive }
                .map {
                    val constraints = constraintRegistry.toConstraints(it.type, it.annotations + it.type.annotations)
                    NestedProperty(
                        name = it.name,
                        jsonName = jsonNamingStrategy.translate(it),
                        codegenType = it.type,
                        type = typeNames.sourceName(it.type),
                        nullable = it.type.isMarkedNullable,
                        required = isRequired(it, constraints),
                        jsonIgnored = it.isJsonIgnored,
                        constraints = constraints,
                        isList = it.type.isCollection,
                        isObject = isObject(it.type),
                    )
                }
        )
    }

    private fun isRequired(member: DeclaredMember, constraints: List<ConstraintMethod>): Boolean {
        if (rejectsNull(constraints)) {
            return true
        }
        return !member.type.isMarkedNullable && !member.hasDefault
    }

}

private fun rejectsNull(constraints: List<ConstraintMethod>): Boolean {
    return constraints.any { constraint -> constraint.descriptor.rejectsNull == true }
}

private fun findItemType(type: CodegenType): CodegenType? {
    if (type.isCollection) {
        return type.typeParams[0]
    }
    if (type.isMap) {
        return type.typeParams[1]
    }
    return null
}

private fun isObject(type: CodegenType): Boolean {
    return (!type.isPrimitive && !type.isCollection && !type.isEnum) || type.isMap
}

private fun isGraphedFunction(function: MemberFn): Boolean {
    return when (MemberFnShape.of(function)) {
        MemberFnShape.VALIDATE_RESULT -> !function.hasImplementation
        MemberFnShape.VALIDATE_CALL -> !function.hasImplementation || !isUntypedJsonFunction(function)
        MemberFnShape.BUILT_IN, MemberFnShape.UNSUPPORTED -> false
    }
}

private fun isUntypedJsonFunction(function: MemberFn): Boolean {
    if (function.validationParamType != null) {
        return false
    }
    val paramFqName = function.paramType.fqName
    return JSON_NODE_PACKAGE_PREFIXES.any { prefix -> paramFqName.startsWith(prefix) }
}
