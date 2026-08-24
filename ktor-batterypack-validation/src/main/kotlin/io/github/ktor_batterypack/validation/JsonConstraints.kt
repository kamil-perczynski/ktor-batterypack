package io.github.ktor_batterypack.validation

import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.JsonNodeType
import tools.jackson.databind.node.ObjectNode
import java.math.BigDecimal
import java.time.LocalDate

object JsonConstraints {

    @JvmStatic
    fun checkObject(prop: String, jsonNode: JsonNode, call: ValidationCall? = null): ObjectNode? {
        val objectNode = jsonNode.get(prop) ?: return null

        if (objectNode.isNull) return null

        if (!objectNode.isObject) {
            call?.typeMismatch(prop, JsonNodeType.OBJECT, objectNode.nodeType)
            return null
        }

        return objectNode.asObject()
    }

    @JvmStatic
    fun checkObject(jsonNode: JsonNode, call: ValidationCall? = null): ObjectNode? {
        if (jsonNode.isNull) return null

        if (!jsonNode.isObject) {
            call?.typeMismatch(JsonNodeType.OBJECT, jsonNode.nodeType)
            return null
        }

        return jsonNode.asObject()
    }

    @JvmStatic
    fun checkArray(prop: String, jsonNode: JsonNode, call: ValidationCall? = null): ArrayNode? {
        val arrayNode = jsonNode.get(prop) ?: return null
        if (!arrayNode.isArray) {
            call?.typeMismatch(prop, JsonNodeType.ARRAY, arrayNode.nodeType)
            return null
        }

        return arrayNode as ArrayNode
    }

    @JvmStatic
    fun checkDouble(prop: String, jsonNode: JsonNode, call: ValidationCall? = null): Double? {
        val numberNode = jsonNode.get(prop) ?: return null
        if (!numberNode.isNumber) {
            call?.typeMismatch(prop, JsonNodeType.NUMBER, numberNode.nodeType)
            return null
        }

        return numberNode.doubleValue()
    }

    @JvmStatic
    fun checkBigDecimal(
        prop: String,
        jsonNode: JsonNode,
        call: ValidationCall? = null
    ): BigDecimal? {
        val numberNode = jsonNode.get(prop) ?: return null
        if (!numberNode.isNumber) {
            call?.typeMismatch(prop, JsonNodeType.NUMBER, numberNode.nodeType)
            return null
        }

        return numberNode.asDecimal()
    }

    @JvmStatic
    fun checkString(prop: String, jsonNode: JsonNode, call: ValidationCall? = null): String? {
        val stringNode = jsonNode.get(prop) ?: return null

        if (stringNode.isNull) return null

        if (!stringNode.isString) {
            call?.typeMismatch(prop, JsonNodeType.STRING, stringNode.nodeType)
            return null
        }

        return stringNode.asString()
    }

    @JvmStatic
    fun checkBoolean(prop: String, jsonNode: JsonNode, call: ValidationCall? = null): Boolean? {
        val booleanNode = jsonNode.get(prop) ?: return null
        if (!booleanNode.isBoolean) {
            call?.typeMismatch(prop, JsonNodeType.BOOLEAN, booleanNode.nodeType)
            return null
        }

        return booleanNode.asBoolean()
    }

    @JvmStatic
    fun checkNotNull(prop: String, node: JsonNode, call: ValidationCall? = null): JsonNode? {
        val propertyNode = node.get(prop)
        if (propertyNode == null || propertyNode.isMissingNode || propertyNode.isNull) {
            call?.propertyError(prop, SingleConstraintError("NotNull", "Must not be null"))
            return null
        }
        return propertyNode
    }

    @JvmStatic
    fun checkNotNull(node: JsonNode, call: ValidationCall? = null): JsonNode? {
        if (node.isMissingNode || node.isNull) {
            call?.directError(SingleConstraintError("NotNull", "Must not be null"))
            return null
        }
        return node
    }

    @JvmStatic
    fun checkNotEmpty(arrayNode: ArrayNode, call: ValidationCall? = null): ArrayNode? {
        if (arrayNode.size() == 0) {
            call?.directError(SingleConstraintError("NotEmpty", "Must not be empty array"))
            return null
        }
        return arrayNode
    }

    @JvmStatic
    fun checkLocalDate(prop: String, node: JsonNode, call: ValidationCall? = null): LocalDate? {
        val value = checkString(prop, node, call) ?: return null
        if (!StringFormats.checkLocalDate(prop, value, call)) return null

        return LocalDate.parse(value)
    }

    @JvmStatic
    fun checkInt(prop: String, jsonNode: JsonNode, call: ValidationCall? = null): Int? {
        val numberNode = jsonNode.get(prop) ?: return null

        if (!numberNode.isNumber) {
            call?.typeMismatch(prop, JsonNodeType.NUMBER, numberNode.nodeType)
            return null
        }

        return numberNode.intValue()
    }

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

private fun ValidationCall.typeMismatch(expected: JsonNodeType, actual: JsonNodeType) {
    directError(
        SingleConstraintError(
            "Type",
            "Expected ${expected.name.lowercase()} but was ${actual.name.lowercase()}"
        )
    )
}
