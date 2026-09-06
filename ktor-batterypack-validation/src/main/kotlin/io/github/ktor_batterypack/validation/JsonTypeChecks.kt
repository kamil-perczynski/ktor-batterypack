package io.github.ktor_batterypack.validation

import tools.jackson.core.io.NumberInput
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.JsonNodeType
import tools.jackson.databind.node.ObjectNode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
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
    fun checkObject(prop: String, jsonNode: JsonNode, call: ValidationCall? = null): ObjectNode? {
        val objectNode = resolveProp(jsonNode, prop) ?: return null

        if (objectNode.isNull) return null

        if (!objectNode.isObject) {
            call?.typeMismatch(prop, JsonNodeType.OBJECT, objectNode.nodeType)
            return null
        }

        return objectNode.asObject()
    }

    @JvmStatic
    fun checkArray(prop: String, jsonNode: JsonNode, call: ValidationCall? = null): ArrayNode? {
        val arrayNode = resolveProp(jsonNode, prop) ?: return null
        if (!arrayNode.isArray) {
            call?.typeMismatch(prop, JsonNodeType.ARRAY, arrayNode.nodeType)
            return null
        }

        return arrayNode as ArrayNode
    }

    @JvmStatic
    fun checkDouble(prop: String, jsonNode: JsonNode, call: ValidationCall? = null): Double? {
        val numberNode = resolveProp(jsonNode, prop) ?: return null

        if (numberNode.isNumber) {
            return numberNode.doubleValue()
        }

        if (numberNode.isString && NumberInput.looksLikeValidNumber(numberNode.asString())) {
            return NumberInput.parseDouble(numberNode.asString(), true)
        }

        call?.typeMismatch(prop, JsonNodeType.NUMBER, numberNode.nodeType)
        return null
    }

    @JvmStatic
    fun checkBigDecimal(prop: String, jsonNode: JsonNode, call: ValidationCall? = null): BigDecimal? {
        val numberNode = resolveProp(jsonNode, prop) ?: return null

        if (numberNode.isNumber) {
            return numberNode.asDecimal()
        }

        if (numberNode.isString && NumberInput.looksLikeValidNumber(numberNode.asString())) {
            return NumberInput.parseBigDecimal(numberNode.asString(), true)
        }

        call?.typeMismatch(prop, JsonNodeType.NUMBER, numberNode.nodeType)
        return null
    }


    @JvmStatic
    fun checkString(prop: String, jsonNode: JsonNode, call: ValidationCall? = null): String? {
        val stringNode = resolveProp(jsonNode, prop)

        if (stringNode == null || stringNode.isNull) return null

        if (!stringNode.isString) {
            call?.typeMismatch(prop, JsonNodeType.STRING, stringNode.nodeType)
            return null
        }

        return stringNode.asString()
    }

    @JvmStatic
    fun checkBoolean(prop: String, jsonNode: JsonNode, call: ValidationCall? = null): Boolean? {
        val booleanNode = resolveProp(jsonNode, prop) ?: return null
        if (!booleanNode.isBoolean) {
            call?.typeMismatch(prop, JsonNodeType.BOOLEAN, booleanNode.nodeType)
            return null
        }

        return booleanNode.asBoolean()
    }

    @JvmStatic
    fun checkNotNull(prop: String, node: JsonNode, call: ValidationCall? = null): JsonNode? {
        val propertyNode = resolveProp(node, prop)
        if (propertyNode == null || propertyNode.isMissingNode || propertyNode.isNull) {
            call?.propertyError(prop, SingleConstraintError("NotNull", "Must not be null"))
            return null
        }
        return propertyNode
    }

    @JvmStatic
    fun checkLocalDate(prop: String, node: JsonNode, call: ValidationCall? = null): LocalDate? {
        val value = checkString(prop, node, call) ?: return null
        if (!StringFormats.checkLocalDate(prop, value, call)) return null

        return LocalDate.parse(value)
    }

    @JvmStatic
    fun checkInt(prop: String, jsonNode: JsonNode, call: ValidationCall? = null): Int? {
        val numberNode = resolveProp(jsonNode, prop) ?: return null

        if (numberNode.isNumber) {
            return numberNode.intValue()
        }

        if (numberNode.isString && NumberInput.looksLikeValidNumber(numberNode.asString())) {
            return NumberInput.parseInt(numberNode.asString())
        }

        call?.typeMismatch(prop, JsonNodeType.NUMBER, numberNode.nodeType)
        return null
    }

    @JvmStatic
    fun checkUuid(prop: String, node: JsonNode, call: ValidationCall? = null): UUID? {
        val value = checkString(prop, node, call) ?: return null
        if (!StringFormats.checkUuid(prop, value, call)) return null

        return UUID.fromString(value)
    }

    @JvmStatic
    fun checkOffsetDateTime(prop: String, node: ObjectNode, call: ValidationCall? = null): OffsetDateTime? {
        val value = checkString(prop, node, call) ?: return null
        if (!StringFormats.checkOffsetDateTime(prop, value, call)) return null

        return OffsetDateTime.parse(value)
    }

    @JvmStatic
    fun checkLong(prop: String, jsonNode: JsonNode, call: ValidationCall? = null): Long? {
        val numberNode = resolveProp(jsonNode, prop) ?: return null

        if (numberNode.isNumber) {
            return numberNode.longValue()
        }

        if (numberNode.isString && NumberInput.looksLikeValidNumber(numberNode.asString())) {
            return NumberInput.parseLong(numberNode.asString())
        }

        call?.typeMismatch(prop, JsonNodeType.NUMBER, numberNode.nodeType)
        return null
    }

}

private fun resolveProp(jsonNode: JsonNode, prop: String): JsonNode? {
    return if (prop == "$") jsonNode else jsonNode.get(prop)
}

private fun ValidationCall.typeMismatch(
    prop: String,
    expected: JsonNodeType,
    actual: JsonNodeType
) {
    propertyError(
        prop,
        SingleConstraintError(
            "Type",
            "Expected ${expected.name.lowercase()} but was ${actual.name.lowercase()}"
        )
    )
}
