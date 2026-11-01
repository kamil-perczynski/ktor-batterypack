package io.github.ktor_batterypack.validation.codegen.helper

import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options
import io.github.ktor_batterypack.validation.ksp.CodegenType
import io.github.ktor_batterypack.validation.ksp.jsonScalarTypeCheckMethodName

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
            return jsonScalarTypeCheckMethodName(type.fqName)
                ?: throw IllegalArgumentException("Unsupported type ${type.fqName}")
        } else {
            return "checkObject"
        }
    }
}
