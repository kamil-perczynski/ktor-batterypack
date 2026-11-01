package io.github.ktor_batterypack.validation.reflection

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import io.github.ktor_batterypack.validation.ksp.MemberFn
import io.github.ktor_batterypack.validation.ksp.ValidatorInterface
import io.github.ktor_batterypack.validation.ksp.ValidatorProperty
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties

class ReflectionValidatorInterface(private val clazz: KClass<*>) : ValidatorInterface {
    override val fqName: String = clazz.qualifiedName!!

    override val name: String = clazz.simpleName!!

    override val className: ClassName
        get() = clazz.asClassName()

    override val symbol: Any?
        get() = null

    override val memberFunctions: List<MemberFn>
        get() = clazz.memberFunctions.map { ReflectionMemberFn(it) }

    override val abstractProperties: List<ValidatorProperty>
        get() = clazz.memberProperties
            .filter { property -> property.isAbstract }
            .map { property ->
                ValidatorProperty(
                    name = property.name,
                    type = toReflectionCodegenType(property.returnType),
                    isMutable = property is KMutableProperty<*>,
                    symbol = null
                )
            }
}
