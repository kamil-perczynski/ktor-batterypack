package io.github.ktor_batterypack.validation.reflection

import io.github.ktor_batterypack.validation.ksp.CodegenType
import io.github.ktor_batterypack.validation.ksp.CodegenAnnotation
import io.github.ktor_batterypack.validation.ksp.DeclaredMember
import io.github.ktor_batterypack.validation.ksp.isPrimitive
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KVisibility
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure

data class ReflectionCodegenType(
    private val kclass: KClass<*>,
    private val ktype: KType
) : CodegenType {

    override val name: String
        get() = kclass.simpleName!!

    override val fqName: String
        get() = kclass.qualifiedName!!

    override val isResolved: Boolean
        get() = ktype.classifier is KClass<*> && ktype.arguments.none { argument -> argument.type == null }

    override val typeParams: List<ReflectionCodegenType> =
        ktype.arguments.mapNotNull { argument -> argument.type }.map { toReflectionCodegenType(it) }

    override val isPrimitive: Boolean
        get() {
            if (!isResolved) {
                return true
            }
            if (isCollection) {
                return false
            }
            if (isMap) {
                return false
            }

            return isPrimitive(ktype.jvmErasure.qualifiedName!!)
        }

    override val isCollection: Boolean
        get() = kclass.isSubclassOf(Collection::class) || (kclass.java.isArray && !kclass.java.componentType.isPrimitive)

    override val isMap: Boolean
        get() = kclass.isSubclassOf(Map::class)

    override val isEnum: Boolean
        get() = kclass.isSubclassOf(Enum::class)

    override val declaredMemberProperties: List<DeclaredMember>
        get() {
            if (isPrimitive) {
                return emptyList()
            }
            if (isCollection) {
                return emptyList()
            }
            if (isMap) {
                return emptyList()
            }
            if (isEnum) {
                return emptyList()
            }

            val primaryConstructor = kclass.primaryConstructor
            return kclass.memberProperties
                .filter { property -> property.visibility == KVisibility.PUBLIC && (property.javaField != null || property.isAbstract) }
                .map { property ->
                    val parameter = primaryConstructor?.parameters?.find { parameter -> parameter.name == property.name }
                    ReflectionDeclaredMember(property, parameter, hasPrimaryConstructor = primaryConstructor != null)
                }
        }

    override val properName: String
        get() {
            if (typeParams.isEmpty()) {
                return name
            }

            return "$name<${typeParams.joinToString(", ") { if (it.isMarkedNullable) it.properName + '?' else it.properName }}>"
        }

    override val annotations: List<CodegenAnnotation>
        get() = ktype.annotations.map { ReflectionCodegenAnnotation(it) }.toList()

    override val isMarkedNullable: Boolean
        get() = ktype.isMarkedNullable

    override val enumValues: List<String>
        get() = kclass.java.enumConstants?.map { it.toString() } ?: emptyList()
}

