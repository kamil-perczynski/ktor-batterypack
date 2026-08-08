package io.github.ktor_batterypack.validation.symbol

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.github.ktor_batterypack.validation.ksp.CodegenType
import io.github.ktor_batterypack.validation.ksp.DeclaredMember
import io.github.ktor_batterypack.validation.ksp.isPrimitive

data class KspCodegenType(private val type: KSType) : CodegenType {

    private val classDecl: KSClassDeclaration = type.declaration as KSClassDeclaration

    override val name: String
        get() = classDecl.simpleName.asString()

    override val fqName: String
        get() = classDecl.qualifiedName?.asString() ?: error("No qualified name for $name")

    override val typeParams: List<KspCodegenType>
        get() = type.arguments.map { toKspCodegenType(it.type!!.resolve()) }

    override val isPrimitive: Boolean
        get() {
            if (isCollection) return false
            if (isMap) return false
            return isPrimitive(fqName, isEnum)
        }

    override val isCollection: Boolean
        get() = isSubtypeOf(classDecl, "kotlin.collections.Collection")

    override val isMap: Boolean
        get() = isSubtypeOf(classDecl, "kotlin.collections.Map")

    private val isEnum: Boolean
        get() = classDecl.classKind == ClassKind.ENUM_CLASS

    override val declaredMemberProperties: List<DeclaredMember>
        get() {
            if (isCollection) return emptyList()
            if (isMap) return emptyList()
            return classDecl.declarations
                .filterIsInstance<KSPropertyDeclaration>()
                .map { KspDeclaredMember(it) }
                .toList()
        }

    override val properName: String
        get() {
            if (typeParams.isEmpty()) return name
            return "$name<${typeParams.joinToString(", ") { it.properName }}>"
        }

}

private fun isSubtypeOf(classDecl: KSClassDeclaration, targetFqName: String): Boolean {
    if (classDecl.qualifiedName?.asString() == targetFqName) return true
    return classDecl.superTypes.any { superType ->
        isSubtypeOf(superType.resolve().declaration as KSClassDeclaration, targetFqName)
    }
}

fun toKspCodegenType(type: KSType): KspCodegenType = KspCodegenType(type)
