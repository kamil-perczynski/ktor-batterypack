package io.github.ktor_batterypack.validation.reflection

import io.github.ktor_batterypack.validation.ksp.CodegenType
import io.github.ktor_batterypack.validation.ksp.CodegenAnnotation
import io.github.ktor_batterypack.validation.ksp.DeclaredMember
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField

data class ReflectionDeclaredMember(val prop: KProperty<*>) : DeclaredMember {

    override val annotations: List<CodegenAnnotation> =
        prop.javaField?.annotations?.map { ReflectionCodegenAnnotation(it) } ?: emptyList()

    override val name: String
        get() = prop.name

    override val type: CodegenType
        get() = toReflectionCodegenType(prop.returnType)

    override val itemType: CodegenType?
        get() {
            if (type.isCollection) {
                return toReflectionCodegenType(prop.returnType.arguments.first().type!!)
            }

            return null
        }

}