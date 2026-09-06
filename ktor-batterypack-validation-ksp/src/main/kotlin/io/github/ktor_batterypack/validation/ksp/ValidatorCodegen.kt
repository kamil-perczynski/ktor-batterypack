package io.github.ktor_batterypack.validation.ksp

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import io.github.ktor_batterypack.validation.ksp.DefaultCodegenNamingConvention.DEFAULT_CODEGEN_NAMING_CONVENTION
import kotlin.to

private const val DEFAULT_CONSTRAINT_TEMPLATE =
    "{{simpleClassName}}.{{methodName}}({{quote prop}}, {{value}}, {{call}}{{#with (constraintArgs args)}}, {{this}}{{/with}})"

class ValidatorCodegen(private val namingConvention: CodegenNamingConvention = DEFAULT_CODEGEN_NAMING_CONVENTION) {

    fun generateValidatorClass(model: CodegenModel): String {
        val handlebars = Handlebars(ClassPathTemplateLoader("/tpl"))
        registerHelpers(handlebars)

        val validatorClass = handlebars.compile("validator-class")

        return validatorClass.apply(Context.newContext(model))
    }

    fun generateJsonValidatorClass(model: CodegenModel): String {
        val handlebars = Handlebars(ClassPathTemplateLoader("/json-tpl"))
        registerHelpers(handlebars)


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
            val paramType = opts.hash<CodegenType>("paramType")

            val prop = opts.hash<NestedProperty>("prop")
            val graph = opts.context.data<CodegenModel>("root")

            if (paramType != null && prop != null && (prop.codegenType.isMap || prop.codegenType.isCollection)) {
                val methodName =
                    namingConvention.collectionValidationMethodName(
                        parentType = paramType,
                        propertyName = prop.name,
                        propertyType = prop.codegenType
                    )

                val matchedMethod = graph.methodsGraph.nodes()
                    .filterIsInstance<CodegenMethod>()
                    .filter { it.name == methodName }
                    .firstOrNull()

                return@Helper matchedMethod?.name
            }

            if (prop == null) {
                val methodName = namingConvention.validationMethodName(paramType)
                val firstOrNull = graph.methodsGraph.nodes()
                    .filterIsInstance<CodegenMethod>()
                    .filter { it.param.properName == paramType.properName }
                    .filter { it.name == methodName }
                    .firstOrNull()

                return@Helper firstOrNull?.name
            }

            val methodName = namingConvention.validationMethodName(prop.codegenType)

            val firstOrNull = graph.methodsGraph.nodes()
                .filterIsInstance<CodegenMethod>()
                .filter { it.param.properName == prop.codegenType.properName }
                .filter { it.name == methodName }
                .firstOrNull()

            return@Helper firstOrNull?.name
        })

        handlebars.registerHelper("toJsonType", Helper<Any> { context, _ ->
            val type = context as CodegenType

            if (type.isMap) {
                return@Helper "ObjectNode"
            } else if (type.isCollection) {
                return@Helper "ArrayNode"
            } else if (type.isEnum) {
                return@Helper "JsonNode"
            } else if (type.isPrimitive) {
                return@Helper "JsonNode"
            } else {
                return@Helper "ObjectNode"
            }
        })

        handlebars.registerHelper("jsonConstraint", Helper<Any> { _, opts ->
            val paramType = opts.hash<ConstraintMethod>("constraint")
            val prop = opts.hash<String>("prop")
            val value = opts.hash<String?>("value") ?: "it"
            val call = opts.hash<String?>("call") ?: "call"

            val tplSrc = paramType.descriptor.jsonCallTpl
                ?: paramType.descriptor.callTpl
                ?: DEFAULT_CONSTRAINT_TEMPLATE
            val tpl = opts.handlebars.compileInline(tplSrc)

            val rendered = tpl.apply(
                mapOf<String, Any>(
                    "simpleClassName" to (paramType.descriptor.simpleClassName ?: ""),
                    "methodName" to paramType.name,
                    "prop" to prop,
                    "value" to value,
                    "call" to call,
                    "args" to paramType.args
                )
            )

            return@Helper rendered
        })

        handlebars.registerHelper("constraint", Helper<Any> { _, opts ->
            val paramType = opts.hash<ConstraintMethod>("constraint")
            val prop = opts.hash<String>("prop")
            val value = opts.hash<String?>("value") ?: "it"
            val call = opts.hash<String?>("call") ?: "call"

            val tplSrc = paramType.descriptor.callTpl ?: DEFAULT_CONSTRAINT_TEMPLATE
            val tpl = opts.handlebars.compileInline(tplSrc)

            val rendered = tpl.apply(
                mapOf<String, Any>(
                    "simpleClassName" to (paramType.descriptor.simpleClassName ?: ""),
                    "methodName" to paramType.name,
                    "prop" to prop,
                    "value" to value,
                    "call" to call,
                    "args" to paramType.args
                )
            )

            return@Helper rendered
        })

        handlebars.registerHelper("constraintArgs", Helper<Any> { ctx, _ ->
            val args = ctx as Map<*, *>
            return@Helper args
                .map { (k, v) ->
                    val value = if (v is String) '"' + v.replace("\\", "\\\\") + '"' else v
                    "$k=$value"
                }
                .joinToString(", ")
        })

        handlebars.registerHelper("toTypeCheckMethodName", Helper<Any> { context, _ ->
            val type = context as CodegenType

            if (type.isMap) {
                return@Helper "checkObject"
            } else if (type.isCollection) {
                return@Helper "checkArray"
            } else if (type.isEnum) {
                return@Helper "checkString"
            } else if (type.isPrimitive) {
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
    }

}