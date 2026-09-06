package io.github.ktor_batterypack.validation.reflection

import io.github.ktor_batterypack.validation.ksp.CodegenAnnotation

class ReflectionCodegenAnnotation(private val annotation: Annotation) : CodegenAnnotation {

    override val name: String
        get() = annotation.annotationClass.simpleName!!
    override val fqName: String
        get() = annotation.annotationClass.qualifiedName!!

    override val args: Map<String, Any>
        get() {
            return annotation.annotationClass.members
                .filter { it.name != "equals" && it.name != "hashCode" && it.name != "toString" && it.name != "payload" && it.name != "groups" && it.name != "message" && it.name != "flags" }
                .associate { it.name to it.call(annotation)!! }
        }
}
