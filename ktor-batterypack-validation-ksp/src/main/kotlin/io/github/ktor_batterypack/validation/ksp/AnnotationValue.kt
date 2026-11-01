package io.github.ktor_batterypack.validation.ksp

sealed interface AnnotationValue {

    val fqName: String

    data class EnumEntry(override val fqName: String) : AnnotationValue {
        override fun toString(): String {
            return fqName
        }
    }

    data class ClassReference(override val fqName: String) : AnnotationValue {
        override fun toString(): String {
            return fqName
        }
    }
}
