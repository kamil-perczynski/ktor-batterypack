package io.github.ktor_batterypack.validation.symbol

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import io.github.ktor_batterypack.validation.ksp.CodegenType
import io.github.ktor_batterypack.validation.ksp.MemberFn

class KspMemberFn(private val fn: KSFunctionDeclaration) : MemberFn {

    override val name: String = fn.simpleName.asString()

    override val paramName: String
        get() = fn.parameters[0].name?.asString() ?: ""

    override val paramType: KspCodegenType
        get() = toKspCodegenType(fn.parameters[0].type.resolve())

    override val returnType: CodegenType?
        get() = fn.returnType?.resolve()?.let { toKspCodegenType(it) }

    override val isPublic: Boolean
        get() = fn.modifiers.none { it in setOf(Modifier.PRIVATE, Modifier.PROTECTED, Modifier.INTERNAL) }
}
