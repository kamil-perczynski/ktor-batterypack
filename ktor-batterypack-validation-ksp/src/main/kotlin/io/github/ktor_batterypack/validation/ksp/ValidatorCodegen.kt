package io.github.ktor_batterypack.validation.ksp

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options
import com.github.jknack.handlebars.io.ClassPathTemplateLoader

class ValidatorCodegen {

    fun generateValidatorClass(model: CodegenModel): String {
        val handlebars = Handlebars(ClassPathTemplateLoader("/tpl"))
        handlebars.with(NoopEscapingStrategy())
        handlebars.registerHelper("quote", Helper<Any> { context, _ ->
            when (context) {
                is String -> '"' + context.replace("\\", "\\\\") + '"'
                else -> context
            }
        })

        val validatorClass = handlebars.compile("validator-class")

        return validatorClass.apply(Context.newContext(model))
    }

}