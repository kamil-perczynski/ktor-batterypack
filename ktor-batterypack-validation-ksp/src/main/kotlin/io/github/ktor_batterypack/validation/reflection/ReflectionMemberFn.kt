package io.github.ktor_batterypack.validation.reflection

import io.github.ktor_batterypack.validation.ksp.MemberFn
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility

class ReflectionMemberFn(private val memberFn: KFunction<*>) : MemberFn {
    override val name: String = memberFn.name

    override val paramName: String
        get() = this.memberFn.parameters[1].name!!

    override val paramType: ReflectionCodegenType
        get() = toReflectionCodegenType(memberFn.parameters[1].type)

    override val returnType: ReflectionCodegenType?
        get() = toReflectionCodegenType(memberFn.returnType)

    override val isPublic: Boolean
        get() = memberFn.visibility == KVisibility.PUBLIC
}