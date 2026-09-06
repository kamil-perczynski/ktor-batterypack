package io.github.ktor_batterypack.validation.codegen.helper

import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options
import io.github.ktor_batterypack.validation.ksp.CodegenType

class ToTypeCheckMethodNameHelper : Helper<Any> {

    companion object {
        const val NAME = "toTypeCheckMethodName"
    }

    override fun apply(context: Any?, options: Options): Any {
        val type = context as CodegenType

        if (type.isMap) {
            return "checkObject"
        } else if (type.isCollection) {
            return "checkArray"
        } else if (type.isEnum) {
            return "checkString"
        } else if (type.isPrimitive) {
            return when (type.fqName) {
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
            return "checkObject"
        }
    }
}
