package io.github.ktor_batterypack.validation.ksp

import com.google.common.graph.GraphBuilder
import com.google.common.graph.MutableGraph
import com.google.common.graph.Traverser
import io.github.ktor_batterypack.validation.codegen.ConstraintRegistry
import io.github.ktor_batterypack.validation.ksp.DefaultCodegenNamingConvention.DEFAULT_CODEGEN_NAMING_CONVENTION

class CodegenModelResolver(
    private val maxDepth: Int = 10,
    private val namingConvention: CodegenNamingConvention = DEFAULT_CODEGEN_NAMING_CONVENTION,
    private val constraintRegistry: ConstraintRegistry = ConstraintRegistry.default(),
    private val jsonNamingStrategy: JsonNamingStrategy = JsonNamingStrategy.IDENTITY
) {

    fun resolve(validatorInterface: ValidatorInterface): CodegenModel {
        val graph = GraphBuilder.directed().build<CodegenNode>()

        val topLevelMethods = findTopLevelMethods(validatorInterface, graph)
        populateTypesGraph(graph)

        val imports = mutableSetOf("io.github.ktor_batterypack.validation.ValidationResult")
        val privateMethods = mutableListOf<PrivateValidationMethod>()
        val publicMethods = mutableListOf<PublicValidationMethod>()

        Traverser.forGraph(graph).breadthFirst(topLevelMethods).forEach { node ->
            when (node) {
                is CodegenMethod -> {
                    imports.add(node.param.fqName)
                    imports.addAll(node.param.typeParams.map { it.fqName })

                    if (node.isPublic && !node.hasImplementation && node.returnType?.fqName == "io.github.ktor_batterypack.validation.ValidationResult") {
                        publicMethods.add(toPublicMethod(node))
                    } else if (!node.isPublic) {
                        val additionalValidationMethods = graph.nodes()
                            .asSequence()
                            .filterIsInstance<CodegenMethod>()
                            .filter { it.isPublic && it.hasImplementation }
                            .filter { it.param.fqName == node.param.fqName }
                            .filter { it.param.properName == node.param.properName }
                            .filter { it.name != node.name }
                            .toList()

                        val privateMethod = toPrivateMethod(node, additionalValidationMethods)
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
            methodsGraph = GuavaMethodsGraph(graph)
        )
    }

    private fun findTopLevelMethods(
        validatorInterface: ValidatorInterface,
        graph: MutableGraph<CodegenNode>
    ): List<CodegenMethod> {
        val topLevelMethods = mutableListOf<CodegenMethod>()

        for (function in validatorInterface.memberFunctions) {
            if (isBuiltInMethod(function)) {
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

    private fun populateTypesGraph(graph: MutableGraph<CodegenNode>) {
        for (i in 1..maxDepth) {
            val types = graph.nodes().filterIsInstance<CodegenType>()
            for (type in types) {
                val properties = type.declaredMemberProperties
                val methods = graph.predecessors(type)
                val methodName = namingConvention.validationMethodName(type)

                val matchingOverride = graph.nodes()
                    .filterIsInstance<CodegenMethod>()
                    .find { method ->
                        method.isPublic
                            && method.name == methodName
                            && method.param.properName == type.properName
                            && method.param.fqName == type.fqName
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
                            && method.param.properName == codegenMethod.param.properName
                            && method.param.fqName == codegenMethod.param.fqName
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
                                    namingConvention.collectionValidationMethodName(type, property),
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

    private fun toPublicMethod(method: CodegenMethod): PublicValidationMethod {
        return PublicValidationMethod(
            name = method.name,
            returnType = method.returnType?.properName!!,
            codegenType = method.param,
            param = ValidationParameter(
                name = method.paramName,
                codegenType = method.param,
                type = method.param.properName,
                fqName = method.param.fqName,
                isList = method.param.isCollection,
                isMap = method.param.isMap,
                isObject = !method.param.isPrimitive && !method.param.isCollection && !method.param.isEnum,
            )
        )
    }

    private fun toPrivateMethod(
        method: CodegenMethod,
        additionalValidationMethods: List<CodegenMethod>
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
                type = method.param.properName,
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
                        type = it.type.properName,
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
                        type = it.type.properName,
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

    private fun isRequired(member: DeclaredMember, constraints: Map<String, ConstraintMethod>): Boolean {
        if (rejectsNull(constraints)) {
            return true
        }
        return !member.type.isMarkedNullable && !member.hasDefault
    }

}

private fun rejectsNull(constraints: Map<String, ConstraintMethod>): Boolean {
    return constraints.values.any { constraint -> constraint.descriptor.rejectsNull == true }
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

private fun isBuiltInMethod(function: MemberFn): Boolean {
    return function.name == "equals" || function.name == "hashCode" || function.name == "toString"
}
