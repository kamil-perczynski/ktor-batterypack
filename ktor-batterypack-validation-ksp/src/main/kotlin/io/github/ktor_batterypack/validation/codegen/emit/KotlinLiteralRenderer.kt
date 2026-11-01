package io.github.ktor_batterypack.validation.codegen.emit

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.joinToCode
import io.github.ktor_batterypack.validation.ksp.AnnotationValue
import io.github.ktor_batterypack.validation.ksp.CodegenAnnotation

object KotlinLiteralRenderer {

    fun render(value: Any?): CodeBlock {
        return when (value) {
            null -> CodeBlock.of("null")
            is String -> CodeBlock.of("%L", '"' + escape(value, '"') + '"')
            is Char -> CodeBlock.of("%L", "'" + escape(value.toString(), '\'') + "'")
            is Long -> CodeBlock.of("%L", renderLong(value))
            is Float -> CodeBlock.of("%L", renderFloat(value))
            is Double -> CodeBlock.of("%L", renderDouble(value))
            is AnnotationValue.EnumEntry -> CodeBlock.of("%T.%N", value.enumClassName, value.entryName)
            is AnnotationValue.ClassReference -> CodeBlock.of("%T::class", value.className)
            is CodegenAnnotation -> renderAnnotation(value)
            is List<*> -> CodeBlock.of("arrayOf(%L)", value.map { item -> render(item) }.joinToCode(", "))
            else -> CodeBlock.of("%L", value.toString())
        }
    }

    fun renderArgs(args: Map<String, Any>): CodeBlock {
        return args.entries
            .map { (name, value) -> CodeBlock.of("%N = %L", name, render(value)) }
            .joinToCode(", ")
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

    private fun renderAnnotation(annotation: CodegenAnnotation): CodeBlock {
        return CodeBlock.of("%T(%L)", annotation.className, renderArgs(annotation.args))
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
                '·' -> builder.append("\\u00B7")
                '♢' -> builder.append("\\u2662")
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
