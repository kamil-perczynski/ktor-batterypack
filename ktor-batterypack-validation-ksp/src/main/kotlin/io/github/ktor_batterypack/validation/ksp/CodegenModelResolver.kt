package io.github.ktor_batterypack.validation.ksp

import com.squareup.kotlinpoet.TypeName
import io.github.ktor_batterypack.validation.codegen.constraint.ConstraintCall
import io.github.ktor_batterypack.validation.codegen.constraint.ConstraintRegistry
import io.github.ktor_batterypack.validation.ksp.CodegenDiagnostic.Severity

private const val MAX_TYPE_METHODS = 1000

private const val JSON_NODE_FQ_NAME = "tools.jackson.databind.JsonNode"
private const val COLLECTION_TOKEN = "kotlin.collections.Collection"
private const val MAP_TOKEN = "kotlin.collections.Map"

private val JSON_NODE_PACKAGE_PREFIXES = listOf("tools.jackson.databind.", "com.fasterxml.jackson.databind.")
private val CONSTRAINT_PACKAGE_PREFIXES = listOf("jakarta.validation.constraints.", "javax.validation.constraints.")
private val CONSTRAINT_META_ANNOTATIONS = setOf("jakarta.validation.Constraint", "javax.validation.Constraint")

class CodegenModelResolver(
    private val constraintRegistry: ConstraintRegistry = ConstraintRegistry.default(),
    private val jsonNamingStrategy: JsonNamingStrategy = JsonNamingStrategy.IDENTITY,
    private val strict: Boolean = false
) {

    companion object {
        const val STRICT_OPTION = "ktor.validation.strict"
    }

    fun resolve(validatorInterface: ValidatorInterface, mode: CodegenMode): ResolvedValidator {
        return Resolution(validatorInterface, mode).resolve()
    }

    private inner class Resolution(
        private val validatorInterface: ValidatorInterface,
        private val mode: CodegenMode
    ) {
        private val names = CodegenNames.of(validatorInterface)
        private val implementedFunctions = validatorInterface.memberFunctions.filter { isImplementedValidation(it) }
        private val requests = mutableMapOf<String, MethodRequest>()
        private val pending = ArrayDeque<MethodRequest>()
        private val diagnostics = linkedSetOf<CodegenDiagnostic>()

        fun resolve(): ResolvedValidator {
            val functions = validatorInterface.memberFunctions
                .filter { function -> !function.hasImplementation && checkFunction(function) }

            for (function in functions) {
                if (MemberFnShape.of(function) == MemberFnShape.VALIDATE_CALL) {
                    val type = modelType(function)
                    val location = parameterLocation(function)
                    checkValue(type, emptyList(), location, function.symbol)
                    schedule(
                        MethodRequest(
                            name = function.name,
                            paramName = function.paramName,
                            type = type,
                            declaredParamType = function.paramType.typeName,
                            location = location,
                            symbol = function.symbol
                        )
                    )
                }
            }

            val entryMethods = functions
                .filter { function -> MemberFnShape.of(function) == MemberFnShape.VALIDATE_RESULT }
                .map { function -> toEntryMethod(function) }

            val typeMethods = mutableListOf<TypeMethod>()
            while (pending.isNotEmpty()) {
                typeMethods.add(toTypeMethod(pending.removeFirst()))
            }

            if (diagnostics.any { diagnostic -> diagnostic.severity == Severity.ERROR }) {
                return ResolvedValidator(null, diagnostics.toList())
            }

            val model = ValidatorModel(
                implClassName = names.implClassName(validatorInterface),
                interfaceClassName = validatorInterface.className,
                mode = mode,
                entryMethods = entryMethods.sortedBy { method -> method.name },
                typeMethods = typeMethods.sortedBy { method -> method.name }
            )
            return ResolvedValidator(model, diagnostics.toList())
        }

        private fun checkFunction(function: MemberFn): Boolean {
            val shape = MemberFnShape.of(function)
            if (shape == MemberFnShape.BUILT_IN) {
                return false
            }
            if (shape == MemberFnShape.UNSUPPORTED) {
                error(
                    "Function `${function.name}` must be declared as `fun ${function.name}(value: T): ValidationResult<T>` " +
                        "or `fun validateT(value: T?, call: ValidationCall)`",
                    function.symbol
                )
                return false
            }
            if (mode == CodegenMode.JSON && function.validationParamType == null) {
                error(
                    "Function `${function.name}` of a @JsonValidator must declare its model type with @ValidationParamType",
                    function.symbol
                )
                return false
            }

            val type = modelType(function)
            if (!type.isResolved) {
                error("Function `${function.name}` validates unsupported type `$type`", function.symbol)
                return false
            }

            if (shape == MemberFnShape.VALIDATE_RESULT) {
                checkResultSignature(function, type)
            } else {
                checkCallSignature(function, type)
            }
            return true
        }

        private fun checkResultSignature(function: MemberFn, type: CodegenType) {
            val expectedFqName = if (mode == CodegenMode.JSON) JSON_NODE_FQ_NAME else type.fqName
            val expectedName = if (mode == CodegenMode.JSON) "JsonNode" else type.typeName.copy(nullable = false).toString()
            val resultType = function.returnType?.typeParams?.firstOrNull()

            if (function.paramType.fqName != expectedFqName || resultType?.fqName != expectedFqName) {
                error(
                    "Function `${function.name}` must be declared as " +
                        "`fun ${function.name}(${function.paramName}: $expectedName): ValidationResult<$expectedName>`",
                    function.symbol
                )
            }
        }

        private fun checkCallSignature(function: MemberFn, type: CodegenType) {
            val expectedName = names.validationMethodName(type)
            if (function.name != expectedName) {
                error(
                    "Function `${function.name}` overrides the validation of `${type.name}` and must be named `$expectedName`",
                    function.symbol
                )
            }
        }

        private fun toEntryMethod(function: MemberFn): EntryMethod {
            val type = modelType(function)
            val location = parameterLocation(function)
            val valid = checkValue(type, emptyList(), location, function.symbol)
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
                    validateWith = if (valid) methodFor(type, location, function.symbol) else null
                )
            )
        }

        private fun toTypeMethod(request: MethodRequest): TypeMethod {
            val type = request.type
            val kind = ValueKind.of(type)
            val typeKey = qualifiedTypeName(type)
            val dispatch = if (kind == ValueKind.OBJECT) toDispatch(type, request.symbol) else null

            val properties = if (kind == ValueKind.OBJECT && validatesOwnProperties(dispatch)) {
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
                item = findItemType(type, kind)?.let { itemType ->
                    toItemModel(itemType, "items of ${request.location}", request.symbol)
                },
                dispatch = dispatch,
                additionalCalls = implementedFunctions
                    .filter { function -> function.name != request.name && qualifiedTypeName(modelType(function)) == typeKey }
                    .map { function -> function.name }
            )
        }

        private fun validatesOwnProperties(dispatch: Dispatch?): Boolean {
            if (dispatch == null) {
                return true
            }
            if (mode == CodegenMode.TYPED) {
                return !dispatch.exhaustive
            }
            return dispatch.branches.any { branch -> branch.validateWith == null }
        }

        private fun toDispatch(type: CodegenType, symbol: Any?): Dispatch? {
            return if (mode == CodegenMode.TYPED) toTypedDispatch(type, symbol) else toJsonDispatch(type, symbol)
        }

        private fun toTypedDispatch(type: CodegenType, symbol: Any?): Dispatch? {
            val branches = PolymorphicTypes.directSubtypes(type)
                .map { subtype -> subtype.type }
                .filter { subtype -> checkSubtype(type, subtype, symbol) }
                .sortedWith(compareByDescending<CodegenType> { subtype -> subtype.supertypes.size }.thenBy { subtype -> subtype.fqName })
                .mapNotNull { subtype ->
                    methodFor(subtype, subtypeLocation(type, subtype), symbol)?.let { method ->
                        DispatchBranch(subtype, emptyList(), method)
                    }
                }
            if (branches.isEmpty()) {
                return null
            }

            val coveredTypes = branches.map { branch -> branch.type.fqName }.toSet()
            return Dispatch(
                typeIdProperty = null,
                branches = branches,
                exhaustive = type.isSealed && type.sealedSubtypes.all { subtype -> subtype.fqName in coveredTypes }
            )
        }

        private fun toJsonDispatch(type: CodegenType, symbol: Any?): Dispatch? {
            val typeInfo = JsonTypeInfoModel.of(type) ?: return null
            val targets = PolymorphicTypes.concreteSubtypes(type)
            if (typeInfo.isDisabled || targets.none { target -> target.type.fqName != type.fqName }) {
                return null
            }
            if (!typeInfo.isSupported) {
                error(
                    "@JsonTypeInfo(use = ${typeInfo.use}, include = ${typeInfo.include}) of `${type.name}` is not supported " +
                        "by a @JsonValidator. Use JsonTypeInfo.Id.NAME with JsonTypeInfo.As.PROPERTY or JsonTypeInfo.As.EXISTING_PROPERTY",
                    symbol
                )
                return null
            }

            val typeIdOwners = mutableMapOf<String, CodegenType>()
            val branches = mutableListOf<DispatchBranch>()
            for (target in targets) {
                val subtype = target.type
                val isOwnType = subtype.fqName == type.fqName
                if (!isOwnType && !checkSubtype(type, subtype, symbol)) {
                    continue
                }

                val typeIds = PolymorphicTypes.typeIds(target, typeInfo)
                for (typeId in typeIds) {
                    val owner = typeIdOwners.putIfAbsent(typeId, subtype)
                    if (owner != null) {
                        error("Type id \"$typeId\" of `${type.name}` maps to both `${owner.name}` and `${subtype.name}`", symbol)
                    }
                }

                val validateWith = if (isOwnType) null else methodFor(subtype, subtypeLocation(type, subtype), symbol)
                if (isOwnType || validateWith != null) {
                    branches.add(DispatchBranch(subtype, typeIds, validateWith))
                }
            }
            return Dispatch(typeInfo.typeIdProperty, branches, exhaustive = false)
        }

        private fun checkSubtype(type: CodegenType, subtype: CodegenType, symbol: Any?): Boolean {
            if (!subtype.isResolved) {
                error("Subtype `${subtype.name}` of `${type.name}` has unsupported type `$subtype`", symbol)
                return false
            }
            if (subtype.supertypes.none { supertype -> supertype.fqName == type.fqName }) {
                error("`${subtype.name}` is listed in @JsonSubTypes of `${type.name}` but does not extend it", symbol)
                return false
            }
            return ValueKind.of(subtype) == ValueKind.OBJECT
        }

        private fun subtypeLocation(type: CodegenType, subtype: CodegenType): String {
            return "subtype `${subtype.name}` of `${type.name}`"
        }

        private fun toPropertyModel(parentType: CodegenType, property: DeclaredMember): PropertyModel {
            val type = property.type
            val kind = ValueKind.of(type)
            val location = "property `${parentType.name}.${property.name}`"
            val annotations = property.annotations + type.annotations
            val valid = checkValue(type, annotations, location, property.symbol)
            val constraints = constraintRegistry.toConstraints(annotations, mode)
            val required = rejectsNull(constraints) || (!type.isMarkedNullable && !property.hasDefault)

            val validateWith = when {
                !valid -> null
                kind == ValueKind.LIST || kind == ValueKind.MAP -> schedule(
                    MethodRequest(
                        name = names.collectionValidationMethodName(parentType, property),
                        paramName = property.name,
                        type = type,
                        declaredParamType = null,
                        location = location,
                        symbol = property.symbol
                    )
                )
                else -> methodFor(type, location, property.symbol)
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

        private fun toItemModel(type: CodegenType, location: String, symbol: Any?): ValueModel {
            val valid = checkValue(type, type.annotations, location, symbol)
            val constraints = constraintRegistry.toConstraints(type.annotations, mode)
            val required = !type.isMarkedNullable || rejectsNull(constraints)

            return ValueModel(
                type = type,
                kind = ValueKind.of(type),
                checkNotNull = checkNotNull(required, type),
                constraints = constraints,
                validateWith = if (valid) methodFor(type, location, symbol) else null
            )
        }

        private fun methodFor(type: CodegenType, location: String, symbol: Any?): String? {
            val kind = ValueKind.of(type)
            if (kind == ValueKind.SCALAR || (kind == ValueKind.ENUM && mode == CodegenMode.JSON)) {
                return null
            }

            val name = names.validationMethodName(type)
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
                    paramName = names.paramName(type),
                    type = type,
                    declaredParamType = null,
                    location = location,
                    symbol = symbol
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

        private fun checkValue(
            type: CodegenType,
            annotations: List<CodegenAnnotation>,
            location: String,
            symbol: Any?
        ): Boolean {
            if (!type.isResolved) {
                error("${capitalize(location)} has unsupported type `$type`", symbol)
                return false
            }

            checkConstraints(type, annotations, location, symbol)

            val kind = ValueKind.of(type)
            if ((kind == ValueKind.LIST || kind == ValueKind.MAP) && findItemType(type, kind) == null) {
                error("${capitalize(location)} has unsupported type `$type`", symbol)
                return false
            }
            if (kind == ValueKind.SCALAR && mode == CodegenMode.JSON && jsonScalarTypeCheckMethodName(type.fqName) == null) {
                error("${capitalize(location)} has type `${type.fqName}`, which a @JsonValidator cannot read from JSON", symbol)
            }
            return true
        }

        private fun checkConstraints(
            type: CodegenType,
            annotations: List<CodegenAnnotation>,
            location: String,
            symbol: Any?
        ) {
            for (annotation in constraintRegistry.unwrapContainerAnnotations(annotations)) {
                val descriptor = constraintRegistry.findDescriptor(annotation.fqName)
                if (descriptor == null) {
                    if (isConstraintAnnotation(annotation)) {
                        report(
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
                    error(
                        "Constraint @${annotation.name} cannot be applied to $location of type `${type.fqName}`. " +
                            "Supported types: ${supportedTypes.joinToString(", ")}",
                        symbol
                    )
                }
            }
        }

        private fun checkNotNull(required: Boolean, type: CodegenType): Boolean {
            return required && (mode == CodegenMode.JSON || type.isMarkedNullable)
        }

        private fun error(message: String, symbol: Any?) {
            report(Severity.ERROR, message, symbol)
        }

        private fun report(severity: Severity, message: String, symbol: Any?) {
            diagnostics.add(CodegenDiagnostic(severity, message, symbol))
        }
    }

    private data class MethodRequest(
        val name: String,
        val paramName: String,
        val type: CodegenType,
        val declaredParamType: TypeName?,
        val location: String,
        val symbol: Any?
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

private fun parameterLocation(function: MemberFn): String {
    return "parameter of `${function.name}`"
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
