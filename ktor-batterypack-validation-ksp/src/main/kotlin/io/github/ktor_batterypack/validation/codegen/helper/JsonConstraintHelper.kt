package io.github.ktor_batterypack.validation.codegen.helper

import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options
import io.github.ktor_batterypack.validation.ksp.ConstraintMethod

class JsonConstraintHelper : Helper<Any> {

    companion object {
        const val NAME = "jsonConstraint"
    }

    override fun apply(context: Any?, options: Options): Any {
        val paramType = options.hash<ConstraintMethod>("constraint")
        val prop = options.hash<String>("prop")
        val value = options.hash<String?>("value") ?: "it"
        val call = options.hash<String?>("call") ?: "call"

        val tplSrc = paramType.descriptor.jsonCallTpl
            ?: paramType.descriptor.callTpl
            ?: DEFAULT_CONSTRAINT_TEMPLATE
        val tpl = options.handlebars.compileInline(tplSrc)

        return tpl.apply(
            ConstraintTemplateContext(
                simpleClassName = paramType.descriptor.simpleClassName ?: "",
                methodName = paramType.name,
                prop = prop,
                value = value,
                call = call,
                args = paramType.args
            )
        )
    }
}
