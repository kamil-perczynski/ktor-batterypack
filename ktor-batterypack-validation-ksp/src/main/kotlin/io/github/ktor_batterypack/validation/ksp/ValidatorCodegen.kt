package io.github.ktor_batterypack.validation.ksp

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.io.ClassPathTemplateLoader

class ValidatorCodegen {

    fun generateValidatorClass(model: CodegenModel): String {
        val handlebars = Handlebars(ClassPathTemplateLoader("/tpl"))
        registerHelpers(handlebars)

        val validatorClass = handlebars.compile("validator-class")

        return validatorClass.apply(Context.newContext(model))
    }

    fun generateJsonValidatorClass(model: CodegenModel): String {
        val handlebars = Handlebars(ClassPathTemplateLoader("/json-tpl"))
        registerHelpers(handlebars)
        handlebars.registerHelper("toJsonType", Helper<Any> { context, _ ->
            val type = context as CodegenType

            if (type.isMap) {
                return@Helper "ObjectNode"
            } else if (type.isCollection) {
                return@Helper "ArrayNode"
            } else if (type.isPrimitive) {
                return@Helper "JsonNode"
            } else {
                return@Helper "ObjectNode"
            }
        })

        handlebars.registerHelper("toTypeCheckMethodName", Helper<Any> { context, _ ->
            val type = context as CodegenType

            if (type.isMap) {
                return@Helper "checkObject"
            } else if (type.isCollection) {
                return@Helper "checkArray"
            } else if (type.isPrimitive) {
                if (type.isEnum) {
                    return@Helper "checkString"
                }

                return@Helper when (type.fqName) {
                    "java.lang.String", "kotlin.String" -> "checkString"
                    "java.lang.Integer", "kotlin.Int" -> "checkInt"
                    "java.lang.Long", "kotlin.Long" -> "checkLong"
                    "java.lang.Float", "kotlin.Float" -> "checkFloat"
                    "java.lang.Double", "kotlin.Double" -> "checkDouble"
                    "java.lang.Boolean", "kotlin.Boolean" -> "checkBoolean"
                    "java.time.LocalDate" -> "checkLocalDate"
                    "java.math.BigDecimal" -> "checkBigDecimal"
                    "java.time.LocalDateTime" -> "checkLocalDateTime"
                    "java.time.LocalTime" -> "checkLocalTime"
                    "java.time.OffsetDateTime" -> "checkOffsetDateTime"
                    "java.time.Instant" -> "checkInstant"
                    "java.util.UUID" -> "checkUuid"
                    else -> throw IllegalArgumentException("Unsupported type ${type.fqName}")
                }
            } else {
                return@Helper "checkObject"
            }
        })

        val validatorClass = handlebars.compile("validator-class")

        return validatorClass.apply(Context.newContext(model))
    }

    private fun registerHelpers(handlebars: Handlebars) {
        handlebars.with(NoopEscapingStrategy())
        handlebars.registerHelper("quote", Helper<Any> { context, _ ->
            when (context) {
                is String -> '"' + context.replace("\\", "\\\\") + '"'
                else -> context
            }
        })

        handlebars.registerHelper("findValidationMethod", Helper<Any> { _, opts ->
            val param = opts.hash<ValidationParameter>("param")
            val prop = opts.hash<NestedProperty>("prop")
            val graph = opts.context.data<CodegenModel>("root")

            if (param != null && prop != null && (prop.codegenType.isMap || prop.codegenType.isCollection)) {
                val methodName =
                    DefaultCodegenNamingConvention.DEFAULT_CODEGEN_NAMING_CONVENTION.collectionValidationMethodName(
                        param.codegenType,
                        prop.name,
                        prop.codegenType
                    )

                val matchedMethod = graph.methodsGraph.nodes()
                    .filterIsInstance<CodegenMethod>()
                    .filter { it.name == methodName }
                    .firstOrNull()

                return@Helper matchedMethod?.name ?: "__NOT_FOUND__"
            }

            if (prop == null) {
                val methodName =
                    DefaultCodegenNamingConvention.DEFAULT_CODEGEN_NAMING_CONVENTION.validationMethodName(param.codegenType)
                val firstOrNull = graph.methodsGraph.nodes()
                    .filterIsInstance<CodegenMethod>()
                    .filter { it.param.properName == param.codegenType.properName }
                    .filter { it.name == methodName }
                    .firstOrNull()

                return@Helper firstOrNull?.name ?: "__NOT_FOUND__"
            }

            val methodName =
                DefaultCodegenNamingConvention.DEFAULT_CODEGEN_NAMING_CONVENTION.validationMethodName(prop.codegenType)

            val firstOrNull = graph.methodsGraph.nodes()
                .filterIsInstance<CodegenMethod>()
                .filter { it.param.properName == prop.codegenType.properName }
                .filter { it.name == methodName }
                .firstOrNull()

            return@Helper firstOrNull?.name ?: "__NOT_FOUND__"
        })


        handlebars.registerHelper("findItemValidationMethod", Helper<Any> { _, opts ->
            val param = opts.hash<ValidationParameter>("param")
            val graph = opts.context.data<CodegenModel>("root")

            val searchedType = if (param.isMap)
                param.codegenType.typeParams[1]
            else if (param.isList)
                param.codegenType.typeParams[0]
            else
                throw IllegalArgumentException("Illegal argument. Must be a map or a list")

            if (searchedType.isPrimitive && !searchedType.isEnum) {
                return@Helper null
            }

            val methodName =
                DefaultCodegenNamingConvention.DEFAULT_CODEGEN_NAMING_CONVENTION.validationMethodName(searchedType)

            val firstOrNull = graph.methodsGraph.nodes()
                .filterIsInstance<CodegenMethod>()
                .filter { it.param.properName == searchedType.properName }
                .filter { it.name == methodName }
                .firstOrNull()

            return@Helper firstOrNull

        })
    }

}