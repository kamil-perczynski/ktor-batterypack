package io.github.ktor_batterypack.validation.reflection

import io.github.ktor_batterypack.validation.ksp.CodegenType
import io.github.ktor_batterypack.validation.ksp.DeclaredMember
import io.github.ktor_batterypack.validation.ksp.isPrimitive
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

data class ReflectionCodegenType(
    private val kclass: KClass<*>,
    private val ktype: KType
) : CodegenType {

    override val name: String
        get() = kclass.simpleName!!

    override val fqName: String
        get() = kclass.qualifiedName!!

    override val typeParams: List<ReflectionCodegenType> =
        ktype.arguments.map { toReflectionCodegenType(it.type!!) }

    override val isPrimitive: Boolean
        get() {
            if (kclass.isSubclassOf(Collection::class)) {
                return false
            }
            if (kclass.isSubclassOf(Map::class)) {
                return false
            }

            return isPrimitive(ktype.jvmErasure.qualifiedName!!, kclass.isSubclassOf(Enum::class))
        }

    override val isCollection: Boolean
        get() = kclass.isSubclassOf(Collection::class)

    override val isMap: Boolean
        get() = kclass.isSubclassOf(Map::class)

    override val declaredMemberProperties: List<DeclaredMember>
        get() {
            if (isCollection) {
                return emptyList()
            }
            if (isMap) {
                return emptyList()
            }

            return kclass.declaredMemberProperties.map { ReflectionDeclaredMember(it) }
        }

    override val properName: String
        get() {
            if (typeParams.isEmpty()) {
                return name
            }

            return "$name<${typeParams.joinToString(", ") { it.properName }}>"
        }
}

