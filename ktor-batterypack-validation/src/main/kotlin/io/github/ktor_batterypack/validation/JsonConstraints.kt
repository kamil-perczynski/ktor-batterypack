package io.github.ktor_batterypack.validation

import tools.jackson.databind.node.ArrayNode

object JsonConstraints {

    @JvmStatic
    fun checkSize(prop: String, node: ArrayNode, call: ValidationCall, min: Int, max: Int) {
        if (node.size() !in min..max) {
            call.propertyError(prop, SingleConstraintError("Size", "Must have size between [$min, $max]"))
        }
    }

    @JvmStatic
    fun checkSize(prop: String, value: String, call: ValidationCall, min: Int, max: Int) {
        return Constraints.checkSize(prop, value, call, min, max)
    }

    @JvmStatic
    fun checkNotEmpty(prop: String, value: String, call: ValidationCall) {
        return Constraints.checkNotEmpty(prop, value, call)
    }

    @JvmStatic
    fun checkNotEmpty(prop: String, value: ArrayNode, call: ValidationCall) {
        if (value.isEmpty) {
            call.propertyError(prop, SingleConstraintError("NotEmpty", "Must not be empty array"))
        }
    }

}
