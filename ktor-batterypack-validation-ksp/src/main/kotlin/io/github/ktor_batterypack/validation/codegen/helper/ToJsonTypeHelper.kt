package io.github.ktor_batterypack.validation.codegen.helper

import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options
import io.github.ktor_batterypack.validation.ksp.CodegenType

class ToJsonTypeHelper : Helper<Any> {

    companion object {
        const val NAME = "toJsonType"
    }

    override fun apply(context: Any?, options: Options): Any {
        val type = context as CodegenType

        if (type.isMap) {
            return "ObjectNode"
        } else if (type.isCollection) {
            return "ArrayNode"
        } else if (type.isEnum) {
            return "JsonNode"
        } else if (type.isPrimitive) {
            return "JsonNode"
        } else {
            return "ObjectNode"
        }
    }
}
