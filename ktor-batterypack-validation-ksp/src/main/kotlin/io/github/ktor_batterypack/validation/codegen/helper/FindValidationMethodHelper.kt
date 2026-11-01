package io.github.ktor_batterypack.validation.codegen.helper

import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options
import io.github.ktor_batterypack.validation.ksp.CodegenMethod
import io.github.ktor_batterypack.validation.ksp.CodegenModel
import io.github.ktor_batterypack.validation.ksp.CodegenNamingConvention
import io.github.ktor_batterypack.validation.ksp.CodegenType
import io.github.ktor_batterypack.validation.ksp.NestedProperty
import io.github.ktor_batterypack.validation.ksp.qualifiedTypeName

class FindValidationMethodHelper(private val namingConvention: CodegenNamingConvention) : Helper<Any> {

    companion object {
        const val NAME = "findValidationMethod"
    }

    override fun apply(context: Any?, options: Options): Any {
        val paramType = options.hash<CodegenType>("paramType")

        val prop = options.hash<NestedProperty>("prop")
        val graph = options.context.data<CodegenModel>("root")

        if (paramType != null && prop != null && (prop.codegenType.isMap || prop.codegenType.isCollection)) {
            val methodName =
                namingConvention.collectionValidationMethodName(
                    parentType = paramType,
                    propertyName = prop.name,
                    propertyType = prop.codegenType,
                    typeNames = graph.typeNames
                )

            val matchedMethod = graph.methodsGraph.nodes()
                .filterIsInstance<CodegenMethod>()
                .filter { it.name == methodName }
                .firstOrNull()

            return matchedMethod?.name ?: ""
        }

        val validatedType = if (prop == null) paramType else prop.codegenType
        val methodName = namingConvention.validationMethodName(validatedType, graph.typeNames)
        val typeKey = qualifiedTypeName(validatedType)

        val firstOrNull = graph.methodsGraph.nodes()
            .filterIsInstance<CodegenMethod>()
            .filter { qualifiedTypeName(it.param) == typeKey }
            .filter { it.name == methodName }
            .firstOrNull()

        return firstOrNull?.name ?: ""
    }
}
