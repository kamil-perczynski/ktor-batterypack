package io.github.ktor_batterypack.validation.reflection

import io.github.ktor_batterypack.validation.ksp.AnnotationValue
import io.github.ktor_batterypack.validation.ksp.CodegenAnnotation
import kotlin.reflect.KClass

class ReflectionCodegenAnnotation(private val annotation: Annotation) : CodegenAnnotation {

    override val name: String
        get() = annotation.annotationClass.simpleName!!
    override val fqName: String
        get() = annotation.annotationClass.qualifiedName!!

    override val args: Map<String, Any>
        get() {
            return annotation.annotationClass.members
                .filter { it.name != "equals" && it.name != "hashCode" && it.name != "toString" && it.name != "payload" && it.name != "groups" && it.name != "message" && it.name != "flags" }
                .associate { it.name to unwrapValue(it.call(annotation)!!) }
        }

    override val metaAnnotations: List<String>
        get() = annotation.annotationClass.annotations.mapNotNull { metaAnnotation -> metaAnnotation.annotationClass.qualifiedName }

    private fun unwrapValue(value: Any): Any {
        return when (value) {
            is Enum<*> -> AnnotationValue.EnumEntry("${value.declaringJavaClass.kotlin.qualifiedName}.${value.name}")
            is KClass<*> -> AnnotationValue.ClassReference(value.qualifiedName!!)
            is Class<*> -> AnnotationValue.ClassReference(value.kotlin.qualifiedName!!)
            is Annotation -> ReflectionCodegenAnnotation(value)
            is Array<*> -> value.filterNotNull().map { item -> unwrapValue(item) }
            is IntArray -> value.toList()
            is LongArray -> value.toList()
            is ShortArray -> value.toList()
            is ByteArray -> value.toList()
            is CharArray -> value.toList()
            is FloatArray -> value.toList()
            is DoubleArray -> value.toList()
            is BooleanArray -> value.toList()
            else -> value
        }
    }
}
