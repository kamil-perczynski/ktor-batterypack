package io.github.ktor_batterypack.validation.reflection

import io.github.ktor_batterypack.validation.ksp.CodegenType
import io.github.ktor_batterypack.validation.ksp.CodegenAnnotation
import io.github.ktor_batterypack.validation.ksp.DeclaredMember
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField

data class ReflectionDeclaredMember(
    val prop: KProperty<*>,
    val constructorParameter: KParameter?,
    val hasPrimaryConstructor: Boolean
) : DeclaredMember {

    override val annotations: List<CodegenAnnotation> =
        listOf(
            prop.javaField?.annotations?.toList() ?: emptyList(),
            prop.annotations,
            prop.getter.annotations,
            constructorParameter?.annotations ?: emptyList(),
        )
            .flatten()
            .map { annotation -> ReflectionCodegenAnnotation(annotation) }
            .distinctBy { annotation -> annotation.fqName }

    override val name: String
        get() = prop.name

    override val type: CodegenType
        get() = toReflectionCodegenType(prop.returnType)

    override val hasDefault: Boolean
        get() {
            if (constructorParameter != null) {
                return constructorParameter.isOptional
            }
            return hasPrimaryConstructor
        }

}
