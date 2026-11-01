package io.github.ktor_batterypack.validation.ksp

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import io.github.ktor_batterypack.validation.codegen.ConstraintCall
import io.github.ktor_batterypack.validation.codegen.ConstraintRegistry
import io.github.ktor_batterypack.validation.ksp.DefaultCodegenNamingConvention.DEFAULT_CODEGEN_NAMING_CONVENTION

private const val MAX_TYPE_METHODS = 1000

private val JSON_NODE_PACKAGE_PREFIXES = listOf("tools.jackson.databind.", "com.fasterxml.jackson.databind.")

class CodegenModelResolver(
    private val namingConvention: CodegenNamingConvention = DEFAULT_CODEGEN_NAMING_CONVENTION,
    private val constraintRegistry: ConstraintRegistry = ConstraintRegistry.default(),
    private val jsonNamingStrategy: JsonNamingStrategy = JsonNamingStrategy.IDENTITY
) {

    fun resolve(validatorInterface: ValidatorInterface, mode: CodegenMode): ValidatorModel {
        return Resolution(validatorInterface, mode).resolve()
    }

    private inner class Resolution(
        private val validatorInterface: ValidatorInterface,
        private val mode: CodegenMode
    ) {
        private val typeNames = CodegenTypeNames.of(validatorInterface)
        private val implementedFunctions = validatorInterface.memberFunctions.filter { isImplementedValidation(it) }
        private val requests = mutableMapOf<String, MethodRequest>()
        private val pending = ArrayDeque<MethodRequest>()

        fun resolve(): ValidatorModel {
            val abstractFunctions = validatorInterface.memberFunctions.filter { function -> !function.hasImplementation }

            for (function in abstractFunctions) {
                if (MemberFnShape.of(function) == MemberFnShape.VALIDATE_CALL) {
                    schedule(
                        MethodRequest(
                            name = function.name,
                            paramName = function.paramName,
                            type = modelType(function),
                            declaredParamType = function.paramType.typeName
                        )
                    )
                }
            }

            val entryMethods = abstractFunctions
                .filter { function -> MemberFnShape.of(function) == MemberFnShape.VALIDATE_RESULT }
                .map { function -> toEntryMethod(function) }

            val typeMethods = mutableListOf<TypeMethod>()
            while (pending.isNotEmpty()) {
                typeMethods.add(toTypeMethod(pending.removeFirst()))
            }

            val interfaceClassName = validatorInterface.className
            return ValidatorModel(
                implClassName = ClassName(interfaceClassName.packageName, namingConvention.implTypeName(validatorInterface)),
                interfaceClassName = interfaceClassName,
                mode = mode,
                entryMethods = entryMethods.sortedBy { method -> method.name },
                typeMethods = typeMethods.sortedBy { method -> method.name }
            )
        }

        private fun toEntryMethod(function: MemberFn): EntryMethod {
            val type = modelType(function)
            return EntryMethod(
                name = function.name,
                paramName = function.paramName,
                paramType = function.paramType.typeName,
                returnType = function.returnType!!.typeName,
                value = ValueModel(
                    type = type,
                    kind = ValueKind.of(type),
                    checkNotNull = mode == CodegenMode.JSON,
                    constraints = emptyList(),
                    validateWith = methodFor(type)
                )
            )
        }

        private fun toTypeMethod(request: MethodRequest): TypeMethod {
            val type = request.type
            val kind = ValueKind.of(type)
            val typeKey = qualifiedTypeName(type)

            val properties = if (kind == ValueKind.OBJECT) {
                type.declaredMemberProperties
                    .filter { property -> mode == CodegenMode.TYPED || !property.isJsonIgnored }
                    .map { property -> toPropertyModel(type, property) }
                    .sortedBy { property -> property.value.kind != ValueKind.SCALAR }
            } else {
                emptyList()
            }

            return TypeMethod(
                name = request.name,
                paramName = request.paramName,
                type = type,
                kind = kind,
                declaredParamType = request.declaredParamType,
                properties = properties,
                item = findItemType(type, kind)?.let { itemType -> toItemModel(itemType) },
                additionalCalls = implementedFunctions
                    .filter { function -> function.name != request.name && qualifiedTypeName(modelType(function)) == typeKey }
                    .map { function -> function.name }
            )
        }

        private fun toPropertyModel(parentType: CodegenType, property: DeclaredMember): PropertyModel {
            val type = property.type
            val kind = ValueKind.of(type)
            val constraints = constraintRegistry.toConstraints(property.annotations + type.annotations, mode)
            val required = rejectsNull(constraints) || (!type.isMarkedNullable && !property.hasDefault)

            val validateWith = if (kind == ValueKind.LIST || kind == ValueKind.MAP) {
                schedule(
                    MethodRequest(
                        name = namingConvention.collectionValidationMethodName(parentType, property, typeNames),
                        paramName = property.name,
                        type = type,
                        declaredParamType = null
                    )
                )
            } else {
                methodFor(type)
            }

            return PropertyModel(
                name = property.name,
                key = if (mode == CodegenMode.JSON) jsonNamingStrategy.translate(property) else property.name,
                value = ValueModel(
                    type = type,
                    kind = kind,
                    checkNotNull = checkNotNull(required, type),
                    constraints = constraints,
                    validateWith = validateWith
                )
            )
        }

        private fun toItemModel(type: CodegenType): ValueModel {
            val constraints = constraintRegistry.toConstraints(type.annotations, mode)
            val required = !type.isMarkedNullable || rejectsNull(constraints)

            return ValueModel(
                type = type,
                kind = ValueKind.of(type),
                checkNotNull = checkNotNull(required, type),
                constraints = constraints,
                validateWith = methodFor(type)
            )
        }

        private fun methodFor(type: CodegenType): String? {
            val kind = ValueKind.of(type)
            if (kind == ValueKind.SCALAR || (kind == ValueKind.ENUM && mode == CodegenMode.JSON)) {
                return null
            }

            val name = namingConvention.validationMethodName(type, typeNames)
            val typeKey = qualifiedTypeName(type)
            if (implementedFunctions.any { function -> function.name == name && qualifiedTypeName(modelType(function)) == typeKey }) {
                return name
            }
            if (kind == ValueKind.ENUM) {
                return null
            }

            return schedule(
                MethodRequest(
                    name = name,
                    paramName = namingConvention.paramName(type),
                    type = type,
                    declaredParamType = null
                )
            )
        }

        private fun schedule(request: MethodRequest): String {
            if (requests.putIfAbsent(request.name, request) == null) {
                check(requests.size <= MAX_TYPE_METHODS) {
                    "Validation model of ${validatorInterface.fqName} needs more than $MAX_TYPE_METHODS methods"
                }
                pending.addLast(request)
            }
            return request.name
        }

        private fun checkNotNull(required: Boolean, type: CodegenType): Boolean {
            return required && (mode == CodegenMode.JSON || type.isMarkedNullable)
        }
    }

    private data class MethodRequest(
        val name: String,
        val paramName: String,
        val type: CodegenType,
        val declaredParamType: TypeName?
    )
}

private fun rejectsNull(constraints: List<ConstraintCall>): Boolean {
    return constraints.any { constraint -> constraint.rejectsNull }
}

private fun findItemType(type: CodegenType, kind: ValueKind): CodegenType? {
    return when (kind) {
        ValueKind.LIST -> type.typeParams.getOrNull(0)
        ValueKind.MAP -> type.typeParams.getOrNull(1)
        else -> null
    }
}

private fun modelType(function: MemberFn): CodegenType {
    return function.validationParamType ?: function.paramType
}

private fun isImplementedValidation(function: MemberFn): Boolean {
    return function.hasImplementation
        && function.isPublic
        && MemberFnShape.of(function) == MemberFnShape.VALIDATE_CALL
        && !isUntypedJsonFunction(function)
}

private fun isUntypedJsonFunction(function: MemberFn): Boolean {
    if (function.validationParamType != null) {
        return false
    }
    val paramFqName = function.paramType.fqName
    return JSON_NODE_PACKAGE_PREFIXES.any { prefix -> paramFqName.startsWith(prefix) }
}
