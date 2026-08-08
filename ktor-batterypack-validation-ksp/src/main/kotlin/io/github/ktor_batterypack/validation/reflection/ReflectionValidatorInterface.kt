package io.github.ktor_batterypack.validation.reflection

import io.github.ktor_batterypack.validation.ksp.MemberFn
import io.github.ktor_batterypack.validation.ksp.ValidatorInterface
import kotlin.reflect.KClass
import kotlin.reflect.full.memberFunctions

class ReflectionValidatorInterface(private val clazz: KClass<*>) : ValidatorInterface {
    override val fqName: String = clazz.qualifiedName!!

    override val name: String = clazz.simpleName!!

    override val memberFunctions: List<MemberFn>
        get() = clazz.memberFunctions.map { ReflectionMemberFn(it) }
}