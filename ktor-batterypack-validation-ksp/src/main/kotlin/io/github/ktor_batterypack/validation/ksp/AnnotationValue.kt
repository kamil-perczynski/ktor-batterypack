package io.github.ktor_batterypack.validation.ksp

import com.squareup.kotlinpoet.ClassName

sealed interface AnnotationValue {

    data class EnumEntry(val enumClassName: ClassName, val entryName: String) : AnnotationValue {
        override fun toString(): String {
            return "${enumClassName.canonicalName}.$entryName"
        }
    }

    data class ClassReference(val className: ClassName) : AnnotationValue {
        override fun toString(): String {
            return className.canonicalName
        }
    }
}
