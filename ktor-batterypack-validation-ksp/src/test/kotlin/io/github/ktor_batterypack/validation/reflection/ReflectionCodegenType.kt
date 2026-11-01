package io.github.ktor_batterypack.validation.reflection

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import io.github.ktor_batterypack.validation.ksp.CodegenSubtype
import io.github.ktor_batterypack.validation.ksp.CodegenType
import io.github.ktor_batterypack.validation.ksp.CodegenAnnotation
import io.github.ktor_batterypack.validation.ksp.DeclaredMember
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KVisibility
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField

data class ReflectionCodegenType(
    private val kclass: KClass<*>,
    private val ktype: KType
) : CodegenType {

    override val name: String
        get() = kclass.simpleName!!

    override val fqName: String
        get() = kclass.qualifiedName!!

    override val typeName: TypeName
        get() = ktype.asTypeName()

    override val isResolved: Boolean
        get() = ktype.classifier is KClass<*> && ktype.arguments.none { argument -> argument.type == null }

    override val typeParams: List<ReflectionCodegenType> =
        ktype.arguments.mapNotNull { argument -> argument.type }.map { toReflectionCodegenType(it) }

    override val isCollection: Boolean
        get() = kclass.isSubclassOf(Collection::class) || (kclass.java.isArray && !kclass.java.componentType.isPrimitive)

    override val isMap: Boolean
        get() = kclass.isSubclassOf(Map::class)

    override val isEnum: Boolean
        get() = kclass.isSubclassOf(Enum::class)

    override val isSealed: Boolean
        get() = kclass.isSealed

    override val isAbstract: Boolean
        get() = kclass.isAbstract || kclass.isSealed || kclass.java.isInterface

    override val declarationAnnotations: List<CodegenAnnotation>
        get() = kclass.annotations.map { ReflectionCodegenAnnotation(it) }

    override val supertypes: List<CodegenType>
        get() = kclass.allSupertypes
            .filter { supertype -> supertype.classifier != Any::class }
            .map { supertype -> toReflectionCodegenType(supertype) }

    override val sealedSubtypes: List<CodegenType>
        get() = kclass.sealedSubclasses.map { subclass -> toReflectionCodegenType(subclass.starProjectedType) }

    override val jsonSubtypes: List<CodegenSubtype>
        get() = kclass.findAnnotation<JsonSubTypes>()?.value.orEmpty().map { entry ->
            CodegenSubtype(
                type = toReflectionCodegenType(entry.value.starProjectedType),
                names = (listOf(entry.name) + entry.names).filter { name -> name.isNotEmpty() }.distinct()
            )
        }

    override val declaredMemberProperties: List<DeclaredMember>
        get() {
            val primaryConstructor = kclass.primaryConstructor
            return kclass.memberProperties
                .filter { property -> property.visibility == KVisibility.PUBLIC && (property.javaField != null || property.isAbstract) }
                .map { property ->
                    val parameter = primaryConstructor?.parameters?.find { parameter -> parameter.name == property.name }
                    ReflectionDeclaredMember(property, parameter, hasPrimaryConstructor = primaryConstructor != null)
                }
        }

    override val annotations: List<CodegenAnnotation>
        get() = ktype.annotations.map { ReflectionCodegenAnnotation(it) }.toList()

    override val isMarkedNullable: Boolean
        get() = ktype.isMarkedNullable

    override val enumValues: List<String>
        get() = kclass.java.enumConstants?.map { it.toString() } ?: emptyList()
}

