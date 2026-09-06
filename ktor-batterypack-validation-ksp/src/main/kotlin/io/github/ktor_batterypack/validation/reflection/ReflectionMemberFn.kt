package io.github.ktor_batterypack.validation.reflection

import io.github.ktor_batterypack.validation.ValidationParamType
import io.github.ktor_batterypack.validation.ksp.CodegenAnnotation
import io.github.ktor_batterypack.validation.ksp.MemberFn
import io.github.ktor_batterypack.validation.ksp.MemberFnParam
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.createType

class ReflectionMemberFn(private val memberFn: KFunction<*>) : MemberFn {

    override val annotations: List<CodegenAnnotation>
        get() {
            return memberFn.annotations.map { ReflectionCodegenAnnotation(it) }
        }

    override val hasImplementation: Boolean
        get() = !memberFn.isAbstract

    override val name: String = memberFn.name

    override val paramName: String
        get() = this.memberFn.parameters[1].name!!

    override val paramType: ReflectionCodegenType
        get() = toReflectionCodegenType(memberFn.parameters[1].type)

    override val params: List<MemberFnParam>
        get() = memberFn.parameters
            .drop(1)
            .map {
                MemberFnParam(it.name!!, toReflectionCodegenType(it.type))
            }

    override val returnType: ReflectionCodegenType
        get() = toReflectionCodegenType(memberFn.returnType)

    override val isPublic: Boolean
        get() = memberFn.visibility == KVisibility.PUBLIC

    override val validationParamType: ReflectionCodegenType?
        get() = memberFn.annotations
            .filterIsInstance<ValidationParamType>()
            .firstOrNull()
            ?.let { toReflectionCodegenType(it.value.createType()) }
}