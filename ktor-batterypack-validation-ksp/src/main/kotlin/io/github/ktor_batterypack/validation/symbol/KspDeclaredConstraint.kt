package io.github.ktor_batterypack.validation.symbol

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType
import io.github.ktor_batterypack.validation.ksp.DeclaredConstraint

class KspDeclaredConstraint(private val annotation: KSAnnotation) : DeclaredConstraint {

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
                // Safely extract argument name (handles null names by resolving from declaration if needed)
                val argName = arg.name?.asString() ?: ""
                argName !in IGNORED_JARARTA_PARAMS && arg.value != null
            }
            .associate { arg ->
                val key = arg.name!!.asString()
                val unwrappedValue = unwrapKspValue(arg.value!!)
                key to unwrappedValue
            }

    private fun unwrapKspValue(value: Any): Any {
        return when (value) {
            is List<*> -> value.filterNotNull().map { unwrapKspValue(it) }
            is KSType -> value.declaration.qualifiedName?.asString() ?: value.toString()
            is KSAnnotation -> KspDeclaredConstraint(value)
            else -> value
        }
    }

    companion object {
        private val IGNORED_JARARTA_PARAMS = setOf("message", "payload", "groups", "flags")
    }
}