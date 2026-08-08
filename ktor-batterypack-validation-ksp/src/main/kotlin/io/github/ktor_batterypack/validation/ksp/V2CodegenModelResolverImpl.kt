package io.github.ktor_batterypack.validation.ksp

import com.google.common.graph.GraphBuilder
import com.google.common.graph.MutableGraph
import com.google.common.graph.Traverser

class V2CodegenModelResolverImpl(private val maxDepth: Int = 10) : V2CodegenModelResolver {

    override fun resolve(validatorInterface: ValidatorInterface): CodegenModel {
        val graph = GraphBuilder.directed().build<CodegenNode>()

        val topLevelMethods = findTopLevelMethods(validatorInterface, graph)
        populateTypesGraph(graph)

        val imports = mutableSetOf("io.github.ktor_batterypack.validation.ValidationResult")
        val privateMethods = mutableListOf<PrivateValidationMethod>()
        val publicMethods = mutableListOf<PublicValidationMethod>()

        val allPrivateMethods = graph.nodes().filterIsInstance<CodegenMethod>()
            .filter { !it.isPublic }

        Traverser.forGraph(graph).breadthFirst(topLevelMethods).forEach { node ->
            when (node) {
                is CodegenMethod -> {
                    imports.add(node.param.fqName)
                    imports.addAll(node.param.typeParams.map { it.fqName })

                    when (node.isPublic) {
                        true -> publicMethods.add(toPublicMethod(node, allPrivateMethods))
                        false -> privateMethods.add(toPrivateMethod(node, allPrivateMethods))
                    }

                    println("Method: ${node.name}(${node.paramName}: ${node.param.properName}): ${node.returnType?.properName}")
                }

                is CodegenType -> throw IllegalStateException("All types should be resolved. Offender: $node")
            }
        }

        return CodegenModel(
            imports = imports.sorted(),
            packageName = validatorInterface.fqName.substringBeforeLast("."),
            interfaceName = validatorInterface.name,
            fqInterfaceName = validatorInterface.fqName,
            typeName = "${validatorInterface.name}Impl",
            fqTypeName = "${validatorInterface.fqName}Impl",
            methods = publicMethods,
            privateMethods = privateMethods
        )
    }

    private fun findTopLevelMethods(
        validatorInterface: ValidatorInterface,
        graph: MutableGraph<CodegenNode>
    ): List<CodegenMethod> {
        val topLevelMethods = mutableListOf<CodegenMethod>()

        for (function in validatorInterface.memberFunctions) {
            if (function.name == "equals" || function.name == "hashCode" || function.name == "toString") {
                continue
            }

            val method = CodegenMethod(
                name = function.name,
                paramName = function.paramName,
                param = function.paramType,
                returnType = function.returnType,
                isPublic = function.isPublic,
            )

            topLevelMethods.add(method)
            graph.addNode(method)
            graph.putEdge(method, function.paramType)
        }

        return topLevelMethods
    }

    private fun populateTypesGraph(graph: MutableGraph<CodegenNode>) {
        for (i in 1..maxDepth) {
            val types = graph.nodes().filterIsInstance<CodegenType>()
            for (type in types) {
                val properties = type.declaredMemberProperties

                val methods = graph.predecessors(type)

                val codegenMethod = CodegenMethod(
                    name = "validate${type.name.replaceFirstChar { it.uppercase() }}",
                    paramName = type.name.replaceFirstChar { it.lowercase() },
                    param = type,
                    isPublic = false,
                )

                val dependentMethods = mutableListOf<CodegenMethod>()
                val dependentTypes = mutableListOf<CodegenType>()

                for (property in properties) {
                    val propName = property.name

                    if (property.type.isCollection || property.type.isMap) {
                        val collectionMethod = CodegenMethod(
                            name = "validate${propName.replaceFirstChar { it.uppercase() }}",
                            paramName = propName,
                            param = property.type,
                            isPublic = false
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
                dependentMethods.forEach { method ->
                    graph.putEdge(codegenMethod, method)
                }
                dependentTypes.forEach { type ->
                    graph.putEdge(codegenMethod, type)
                }

                graph.removeNode(type)
            }
        }
    }

    private fun toPublicMethod(
        method: CodegenMethod,
        allPrivateMethods: List<CodegenMethod>
    ): PublicValidationMethod {
        val privateMethod = allPrivateMethods.find { it.param == method.param }
            ?: throw IllegalStateException("Private method not found: ${method.param}")

        return PublicValidationMethod(
            name = method.name,
            returnType = method.returnType?.properName!!,
            param = ValidationParameter(
                name = method.paramName,
                type = method.param.properName,
                fqName = method.param.fqName,
                isList = method.param.isCollection,
                isObject = !method.param.isPrimitive && !method.param.isCollection,
            ),
            validationMethodName = privateMethod.name,
        )
    }

    private fun toPrivateMethod(
        method: CodegenMethod,
        allPrivateMethods: List<CodegenMethod>
    ): PrivateValidationMethod {
        return PrivateValidationMethod(
            name = method.name,
            param = ValidationParameter(
                name = method.paramName,
                type = method.param.properName,
                fqName = method.param.fqName,
                isList = method.param.isCollection,
                isObject = isObject(method.param),
                _itemValidatorMethodName =
                    if (method.param.isCollection)
                        if (!method.param.typeParams.first().isPrimitive)
                            "validate${method.param.typeParams.first().name.replaceFirstChar { name -> name.uppercase() }}"
                        else ""
                    else
                        null
            ),
            directProperties = method.param.declaredMemberProperties
                .filter { it.type.isPrimitive }
                .map {
                    val constraints = toConstraints(it)
                    DirectProperties(
                        name = it.name,
                        type = it.type.properName,
                        nullable = true,
                        constraints = constraints,
                    )
                },
            nestedProperties = method.param.declaredMemberProperties
                .filter { !it.type.isPrimitive || it.type.isMap }
                .map {
                    val constraints = toConstraints(it)
                    NestedProperty(
                        name = it.name,
                        type = it.type.properName,
                        nullable = true,
                        constraints = constraints,
                        isList = it.type.isCollection,
                        isObject = isObject(it.type),
                        _itemValidatorMethodName =
                            if (it.type.isCollection)
                                allPrivateMethods.find { prvMethod -> prvMethod.param.typeParams.firstOrNull() == it.type }?.name
                            else
                                allPrivateMethods.find { prvMethod -> prvMethod.param == it.type }?.name,
                    )
                }
        )
    }

    private fun toConstraints(member: DeclaredMember): Map<String, ConstraintMethod> {
        return member.constraints.associate { constraint ->
            constraint.name to ConstraintMethod(
                "check${constraint.name}",
                constraint.args
            )
        }
    }

    private fun isObject(type: CodegenType): Boolean {
        return (!type.isPrimitive && !type.isCollection) || type.isMap
    }
}
