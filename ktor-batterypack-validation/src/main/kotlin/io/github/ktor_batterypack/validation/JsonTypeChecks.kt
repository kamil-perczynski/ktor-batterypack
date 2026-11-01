package io.github.ktor_batterypack.validation

import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.JsonNodeType
import tools.jackson.databind.node.ObjectNode
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.UUID

object JsonTypeChecks {

    @JvmStatic
    fun checkEnum(prop: String, value: String, call: ValidationCall?, allowedValues: Set<String>) {
        if (!allowedValues.contains(value)) {
            call?.propertyError(
                prop,
                SingleConstraintError("EnumValues", "Allowed values are: $allowedValues")
            )
        }
    }

    @JvmStatic
    fun checkTypeId(prop: String, node: JsonNode, call: ValidationCall?, allowedValues: Set<String>): String? {
        checkNotNull(prop, node, call) ?: return null
        val typeId = checkString(prop, node, call) ?: return null
        if (!allowedValues.contains(typeId)) {
            checkEnum(prop, typeId, call, allowedValues)
            return null
        }
        return typeId
    }

    @JvmStatic
    fun checkObject(prop: String, jsonNode: JsonNode, call: ValidationCall? = null): ObjectNode? {
        val objectNode = resolveValue(jsonNode, prop) ?: return null

        if (!objectNode.isObject) {
            typeMismatch(call, prop, JsonNodeType.OBJECT, objectNode.nodeType)
            return null
        }

        return objectNode.asObject()
    }

    @JvmStatic
    fun checkArray(prop: String, jsonNode: JsonNode, call: ValidationCall? = null): ArrayNode? {
        val arrayNode = resolveValue(jsonNode, prop) ?: return null

        if (!arrayNode.isArray) {
            typeMismatch(call, prop, JsonNodeType.ARRAY, arrayNode.nodeType)
            return null
        }

        return arrayNode as ArrayNode
    }

    @JvmStatic
    fun checkInt(prop: String, jsonNode: JsonNode, call: ValidationCall? = null): Int? {
        val node = resolveValue(jsonNode, prop) ?: return null

        if (node.isNumber) {
            if (!node.isIntegralNumber) {
                integerExpected(call, prop)
                return null
            }
            val value = node.intValueOpt()
            if (value.isEmpty) {
                outOfRange(call, prop, "integer")
                return null
            }
            return value.asInt
        }

        if (node.isString) {
            val text = node.asString()
            val value = text.toIntOrNull()
            if (value == null) {
                if (text.toBigIntegerOrNull() != null) outOfRange(call, prop, "integer")
                else integerExpected(call, prop)
            }
            return value
        }

        typeMismatch(call, prop, JsonNodeType.NUMBER, node.nodeType)
        return null
    }

    @JvmStatic
    fun checkLong(prop: String, jsonNode: JsonNode, call: ValidationCall? = null): Long? {
        val node = resolveValue(jsonNode, prop) ?: return null

        if (node.isNumber) {
            if (!node.isIntegralNumber) {
                integerExpected(call, prop)
                return null
            }
            val value = node.longValueOpt()
            if (value.isEmpty) {
                outOfRange(call, prop, "long")
                return null
            }
            return value.asLong
        }

        if (node.isString) {
            val text = node.asString()
            val value = text.toLongOrNull()
            if (value == null) {
                if (text.toBigIntegerOrNull() != null) outOfRange(call, prop, "long")
                else integerExpected(call, prop)
            }
            return value
        }

        typeMismatch(call, prop, JsonNodeType.NUMBER, node.nodeType)
        return null
    }

    @JvmStatic
    fun checkFloat(prop: String, jsonNode: JsonNode, call: ValidationCall? = null): Float? {
        val value = checkDouble(prop, jsonNode, call) ?: return null

        if (value.isFinite() && (value > Float.MAX_VALUE || value < -Float.MAX_VALUE)) {
            outOfRange(call, prop, "float")
            return null
        }

        return value.toFloat()
    }

    @JvmStatic
    fun checkDouble(prop: String, jsonNode: JsonNode, call: ValidationCall? = null): Double? {
        val node = resolveValue(jsonNode, prop) ?: return null

        if (node.isNumber) {
            val value = node.doubleValueOpt()
            if (value.isEmpty || !value.asDouble.isFinite()) {
                outOfRange(call, prop, "double")
                return null
            }
            return value.asDouble
        }

        if (node.isString) {
            val value = node.asString().toDoubleOrNull()
            if (value == null || !value.isFinite()) {
                numberExpected(call, prop)
                return null
            }
            return value
        }

        typeMismatch(call, prop, JsonNodeType.NUMBER, node.nodeType)
        return null
    }

    @JvmStatic
    fun checkBigDecimal(prop: String, jsonNode: JsonNode, call: ValidationCall? = null): BigDecimal? {
        val node = resolveValue(jsonNode, prop) ?: return null

        if (node.isNumber) {
            val value = node.decimalValueOpt()
            if (value.isEmpty) {
                numberExpected(call, prop)
                return null
            }
            return value.get()
        }

        if (node.isString) {
            val value = node.asString().toBigDecimalOrNull()
            if (value == null) {
                numberExpected(call, prop)
            }
            return value
        }

        typeMismatch(call, prop, JsonNodeType.NUMBER, node.nodeType)
        return null
    }

    @JvmStatic
    fun checkString(prop: String, jsonNode: JsonNode, call: ValidationCall? = null): String? {
        val stringNode = resolveValue(jsonNode, prop) ?: return null

        if (!stringNode.isString) {
            typeMismatch(call, prop, JsonNodeType.STRING, stringNode.nodeType)
            return null
        }

        return stringNode.asString()
    }

    @JvmStatic
    fun checkBoolean(prop: String, jsonNode: JsonNode, call: ValidationCall? = null): Boolean? {
        val booleanNode = resolveValue(jsonNode, prop) ?: return null

        if (!booleanNode.isBoolean) {
            typeMismatch(call, prop, JsonNodeType.BOOLEAN, booleanNode.nodeType)
            return null
        }

        return booleanNode.asBoolean()
    }

    @JvmStatic
    fun checkNotNull(prop: String, node: JsonNode, call: ValidationCall? = null): JsonNode? {
        val propertyNode = resolveValue(node, prop)
        if (propertyNode == null) {
            call?.propertyError(prop, SingleConstraintError("NotNull", "Must not be null"))
            return null
        }
        return propertyNode
    }

    @JvmStatic
    fun checkUuid(prop: String, node: JsonNode, call: ValidationCall? = null): UUID? {
        val value = checkString(prop, node, call) ?: return null
        if (!StringFormats.checkUuid(prop, value, call)) return null

        return UUID.fromString(value)
    }

    @JvmStatic
    fun checkLocalDate(prop: String, node: JsonNode, call: ValidationCall? = null): LocalDate? {
        val value = checkString(prop, node, call) ?: return null
        if (!StringFormats.checkLocalDate(prop, value, call)) return null

        return parseOrNull(call, prop, "local date") { LocalDate.parse(value) }
    }

    @JvmStatic
    fun checkLocalDateTime(prop: String, node: JsonNode, call: ValidationCall? = null): LocalDateTime? {
        val value = checkString(prop, node, call) ?: return null
        if (!StringFormats.checkLocalDateTime(prop, value, call)) return null

        return parseOrNull(call, prop, "local date-time") { LocalDateTime.parse(value) }
    }

    @JvmStatic
    fun checkLocalTime(prop: String, node: JsonNode, call: ValidationCall? = null): LocalTime? {
        val value = checkString(prop, node, call) ?: return null
        if (!StringFormats.checkLocalTime(prop, value, call)) return null

        return parseOrNull(call, prop, "local time") { LocalTime.parse(value) }
    }

    @JvmStatic
    fun checkOffsetDateTime(prop: String, node: JsonNode, call: ValidationCall? = null): OffsetDateTime? {
        val value = checkString(prop, node, call) ?: return null
        if (!StringFormats.checkOffsetDateTime(prop, value, call)) return null

        return parseOrNull(call, prop, "offset date-time") { OffsetDateTime.parse(value) }
    }

    @JvmStatic
    fun checkInstant(prop: String, node: JsonNode, call: ValidationCall? = null): Instant? {
        val value = checkString(prop, node, call) ?: return null
        if (!StringFormats.checkInstant(prop, value, call)) return null

        return parseOrNull(call, prop, "instant") { Instant.parse(value) }
    }

}

private fun resolveValue(jsonNode: JsonNode, prop: String): JsonNode? {
    val node = if (prop == "$") jsonNode else jsonNode.get(prop)
    if (node == null || node.isNull || node.isMissingNode) {
        return null
    }
    return node
}

private fun <T> parseOrNull(call: ValidationCall?, prop: String, formatName: String, parse: () -> T): T? {
    return try {
        parse()
    } catch (_: DateTimeParseException) {
        call?.propertyError(prop, SingleConstraintError("Format", "Must be a valid $formatName"))
        null
    }
}

private fun typeMismatch(call: ValidationCall?, prop: String, expected: JsonNodeType, actual: JsonNodeType) {
    call?.propertyError(
        prop,
        SingleConstraintError(
            "Type",
            "Expected ${expected.name.lowercase()} but was ${actual.name.lowercase()}"
        )
    )
}

private fun integerExpected(call: ValidationCall?, prop: String) {
    call?.propertyError(prop, SingleConstraintError("Type", "Expected integer"))
}

private fun numberExpected(call: ValidationCall?, prop: String) {
    call?.propertyError(prop, SingleConstraintError("Type", "Expected number"))
}

private fun outOfRange(call: ValidationCall?, prop: String, typeName: String) {
    call?.propertyError(prop, SingleConstraintError("Range", "Value is out of range for $typeName"))
}
