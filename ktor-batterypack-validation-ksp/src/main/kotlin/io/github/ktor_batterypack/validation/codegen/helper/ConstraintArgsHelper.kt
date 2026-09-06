package io.github.ktor_batterypack.validation.codegen.helper

import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options

class ConstraintArgsHelper : Helper<Any> {

    companion object {
        const val NAME = "constraintArgs"
    }

    override fun apply(context: Any?, options: Options): Any {
        val args = context as Map<*, *>
        return args
            .map { (k, v) ->
                val value = if (v is String) '"' + v.replace("\\", "\\\\") + '"' else v
                "$k=$value"
            }
            .joinToString(", ")
    }
}
