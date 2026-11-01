package io.github.ktor_batterypack.validation.symbol

import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.symbol.Variance
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import io.github.ktor_batterypack.validation.ksp.CodegenAnnotation
import io.github.ktor_batterypack.validation.ksp.CodegenType
import io.github.ktor_batterypack.validation.ksp.DeclaredMember
import io.github.ktor_batterypack.validation.ksp.isPrimitive

class KspCodegenType(
    private val type: KSType,
    private val typeAnnotations: List<KSAnnotation> = type.annotations.toList()
) : CodegenType {

    private val classDecl: KSClassDeclaration?
        get() {
            if (type.isError) {
                return null
            }
            return type.declaration as? KSClassDeclaration
        }

    val containingFile: KSFile?
        get() = classDecl?.containingFile

    override val name: String
        get() = type.declaration.simpleName.asString()

    override val fqName: String
        get() = type.declaration.qualifiedName?.asString() ?: name

    override val typeName: TypeName
        get() = type.toTypeName()

    override val isResolved: Boolean
        get() {
            if (classDecl == null) {
                return false
            }
            return type.arguments.none { argument -> argument.variance == Variance.STAR || argument.type == null }
        }

    override val typeParams: List<KspCodegenType>
        get() = type.arguments.mapNotNull { argument -> argument.type?.resolve() }.map { toKspCodegenType(it) }

    override val isPrimitive: Boolean
        get() {
            if (!isResolved) return true
            if (isCollection) return false
            if (isMap) return false
            return isPrimitive(fqName)
        }

    override val isCollection: Boolean
        get() {
            val declaration = classDecl ?: return false
            return fqName == "kotlin.Array" || isSubtypeOf(declaration, "kotlin.collections.Collection")
        }

    override val isMap: Boolean
        get() {
            val declaration = classDecl ?: return false
            return isSubtypeOf(declaration, "kotlin.collections.Map")
        }

    override val isEnum: Boolean
        get() = classDecl?.classKind == ClassKind.ENUM_CLASS

    override val declaredMemberProperties: List<DeclaredMember>
        get() {
            val declaration = classDecl ?: return emptyList()
            if (isCollection) return emptyList()
            if (isMap) return emptyList()
            if (isPrimitive) return emptyList()
            if (isEnum) return emptyList()

            if (declaration.origin == Origin.JAVA || declaration.origin == Origin.JAVA_LIB) {
                return declaration.declarations
                    .filterIsInstance<KSPropertyDeclaration>()
                    .map { property -> KspDeclaredMember(property, null, hasPrimaryConstructor = false) }
                    .toList()
            }

            val primaryConstructor = declaration.primaryConstructor
            return declaration.getAllProperties()
                .filter { property -> property.isPublic() && (property.hasBackingField || property.isAbstract()) }
                .map { property ->
                    val parameter = primaryConstructor?.parameters?.find { parameter ->
                        parameter.name?.asString() == property.simpleName.asString()
                    }
                    KspDeclaredMember(property, parameter, hasPrimaryConstructor = primaryConstructor != null)
                }
                .toList()
        }

    override val annotations: List<CodegenAnnotation>
        get() = typeAnnotations.map { KspCodegenAnnotation(it) }

    override val isMarkedNullable: Boolean
        get() = type.isMarkedNullable

    override val enumValues: List<String>
        get() = classDecl?.declarations
            ?.filterIsInstance<KSClassDeclaration>()
            ?.filter { it.classKind == ClassKind.ENUM_ENTRY }
            ?.map { it.simpleName.asString() }
            ?.toList()
            .orEmpty()

    override fun equals(other: Any?): Boolean {
        return other is KspCodegenType && other.type == type
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }

    override fun toString(): String {
        return type.toString()
    }

}

private fun isSubtypeOf(classDecl: KSClassDeclaration, targetFqName: String): Boolean {
    if (classDecl.qualifiedName?.asString() == targetFqName) return true
    return classDecl.superTypes.any { superType ->
        val superDecl = superType.resolve().declaration as? KSClassDeclaration
        superDecl != null && isSubtypeOf(superDecl, targetFqName)
    }
}

fun toKspCodegenType(type: KSType): KspCodegenType {
    val annotations = type.annotations.toMutableList()
    var expanded = type
    var nullable = type.isMarkedNullable
    while (true) {
        val alias = expanded.declaration as? KSTypeAlias ?: break
        if (alias.typeParameters.isNotEmpty()) {
            break
        }
        expanded = alias.type.resolve()
        nullable = nullable || expanded.isMarkedNullable
        annotations += expanded.annotations
    }
    if (nullable && !expanded.isMarkedNullable) {
        expanded = expanded.makeNullable()
    }
    return KspCodegenType(expanded, annotations)
}
