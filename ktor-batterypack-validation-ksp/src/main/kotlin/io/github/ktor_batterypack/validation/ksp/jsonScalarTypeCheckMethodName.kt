package io.github.ktor_batterypack.validation.ksp

fun jsonScalarTypeCheckMethodName(fqName: String): String? {
    return when (fqName) {
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
        else -> null
    }
}
