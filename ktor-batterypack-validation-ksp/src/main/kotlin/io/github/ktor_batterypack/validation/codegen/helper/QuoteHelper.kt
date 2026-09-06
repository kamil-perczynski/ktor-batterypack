package io.github.ktor_batterypack.validation.codegen.helper

import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options

class QuoteHelper : Helper<Any> {

    companion object {
        const val NAME = "quote"
    }

    override fun apply(context: Any?, options: Options): Any {
        return when (context) {
            is String -> '"' + context.replace("\\", "\\\\") + '"'
            else -> context ?: ""
        }
    }
}
