package io.github.ktor_batterypack.validation.codegen.helper

import io.github.ktor_batterypack.validation.ksp.AnnotationValue
import io.github.ktor_batterypack.validation.ksp.CodegenAnnotation

object KotlinLiteralRenderer {

    fun render(value: Any?): String {
        return when (value) {
            null -> "null"
            is String -> '"' + escape(value, '"') + '"'
            is Char -> "'" + escape(value.toString(), '\'') + "'"
            is Long -> renderLong(value)
            is Float -> renderFloat(value)
            is Double -> renderDouble(value)
            is AnnotationValue.EnumEntry -> value.fqName
            is AnnotationValue.ClassReference -> "${value.fqName}::class"
            is CodegenAnnotation -> renderAnnotation(value)
            is List<*> -> value.joinToString(", ", "arrayOf(", ")") { item -> render(item) }
            else -> value.toString()
        }
    }

    private fun renderLong(value: Long): String {
        if (value == Long.MIN_VALUE) {
            return "Long.MIN_VALUE"
        }
        return value.toString()
    }

    private fun renderFloat(value: Float): String {
        return when {
            value.isNaN() -> "Float.NaN"
            value == Float.POSITIVE_INFINITY -> "Float.POSITIVE_INFINITY"
            value == Float.NEGATIVE_INFINITY -> "Float.NEGATIVE_INFINITY"
            else -> "${value}f"
        }
    }

    private fun renderDouble(value: Double): String {
        return when {
            value.isNaN() -> "Double.NaN"
            value == Double.POSITIVE_INFINITY -> "Double.POSITIVE_INFINITY"
            value == Double.NEGATIVE_INFINITY -> "Double.NEGATIVE_INFINITY"
            else -> value.toString()
        }
    }

    private fun renderAnnotation(annotation: CodegenAnnotation): String {
        return annotation.args.entries.joinToString(", ", "${annotation.fqName}(", ")") { (name, value) ->
            "$name = ${render(value)}"
        }
    }

    private fun escape(value: String, quote: Char): String {
        val builder = StringBuilder(value.length)
        for ((index, char) in value.withIndex()) {
            when (char) {
                '\\' -> builder.append("\\\\")
                '$' -> builder.append(if (startsTemplate(value, index + 1)) "\\$" else "$")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                quote -> builder.append('\\').append(quote)
                else -> builder.append(char)
            }
        }
        return builder.toString()
    }

    private fun startsTemplate(value: String, index: Int): Boolean {
        if (index >= value.length) {
            return false
        }
        val next = value[index]
        return next.isLetter() || next == '_' || next == '{' || next == '`'
    }
}
