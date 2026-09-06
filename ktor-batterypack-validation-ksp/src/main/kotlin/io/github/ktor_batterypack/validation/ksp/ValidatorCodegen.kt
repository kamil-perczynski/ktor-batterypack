package io.github.ktor_batterypack.validation.ksp

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import io.github.ktor_batterypack.validation.codegen.helper.ConstraintArgsHelper
import io.github.ktor_batterypack.validation.codegen.helper.ConstraintHelper
import io.github.ktor_batterypack.validation.codegen.helper.FindValidationMethodHelper
import io.github.ktor_batterypack.validation.codegen.helper.JsonConstraintHelper
import io.github.ktor_batterypack.validation.codegen.helper.QuoteHelper
import io.github.ktor_batterypack.validation.codegen.helper.ToJsonTypeHelper
import io.github.ktor_batterypack.validation.codegen.helper.ToTypeCheckMethodNameHelper
import io.github.ktor_batterypack.validation.ksp.DefaultCodegenNamingConvention.DEFAULT_CODEGEN_NAMING_CONVENTION

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
        handlebars.registerHelper(QuoteHelper.NAME, QuoteHelper())
        handlebars.registerHelper(
            FindValidationMethodHelper.NAME,
            FindValidationMethodHelper(namingConvention)
        )
        handlebars.registerHelper(ToJsonTypeHelper.NAME, ToJsonTypeHelper())
        handlebars.registerHelper(JsonConstraintHelper.NAME, JsonConstraintHelper())
        handlebars.registerHelper(ConstraintHelper.NAME, ConstraintHelper())
        handlebars.registerHelper(ConstraintArgsHelper.NAME, ConstraintArgsHelper())
        handlebars.registerHelper(ToTypeCheckMethodNameHelper.NAME, ToTypeCheckMethodNameHelper())
    }

}
