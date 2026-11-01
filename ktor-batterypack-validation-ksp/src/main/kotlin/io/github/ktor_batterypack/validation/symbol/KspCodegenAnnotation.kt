package io.github.ktor_batterypack.validation.symbol

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.github.ktor_batterypack.validation.ksp.AnnotationValue
import io.github.ktor_batterypack.validation.ksp.CodegenAnnotation

class KspCodegenAnnotation(private val annotation: KSAnnotation) : CodegenAnnotation {

    private val resolvedDeclaration by lazy {
        annotation.annotationType.resolve().declaration
    }

    override val name: String
        get() {
            return resolvedDeclaration.simpleName.asString()
        }

    override val fqName: String
        get() = resolvedDeclaration.qualifiedName?.asString() ?: name

    override val args: Map<String, Any>
        get() = annotation.arguments
            .filter { arg ->
                val argName = arg.name?.asString() ?: ""
                argName !in IGNORED_JARARTA_PARAMS && arg.value != null
            }
            .associate { arg ->
                val key = arg.name!!.asString()
                val unwrappedValue = unwrapKspValue(arg.value!!)
                key to unwrappedValue
            }

    override val metaAnnotations: List<String>
        get() = resolvedDeclaration.annotations
            .mapNotNull { metaAnnotation -> metaAnnotation.annotationType.resolve().declaration.qualifiedName?.asString() }
            .toList()

    private fun unwrapKspValue(value: Any): Any {
        return when (value) {
            is List<*> -> value.filterNotNull().map { unwrapKspValue(it) }
            is KSType -> toAnnotationValue(value.declaration)
            is KSClassDeclaration -> toAnnotationValue(value)
            is KSAnnotation -> KspCodegenAnnotation(value)
            else -> value
        }
    }

    private fun toAnnotationValue(declaration: KSDeclaration): AnnotationValue {
        val qualifiedName = declaration.qualifiedName?.asString() ?: declaration.simpleName.asString()
        if (declaration is KSClassDeclaration && declaration.classKind == ClassKind.ENUM_ENTRY) {
            return AnnotationValue.EnumEntry(qualifiedName)
        }
        return AnnotationValue.ClassReference(qualifiedName)
    }

    companion object {
        private val IGNORED_JARARTA_PARAMS = setOf("message", "payload", "groups", "flags")
    }
}
