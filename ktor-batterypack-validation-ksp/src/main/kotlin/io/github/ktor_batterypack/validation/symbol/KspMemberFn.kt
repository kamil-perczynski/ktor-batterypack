package io.github.ktor_batterypack.validation.symbol

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import io.github.ktor_batterypack.validation.ksp.CodegenType
import io.github.ktor_batterypack.validation.ksp.CodegenAnnotation
import io.github.ktor_batterypack.validation.ksp.MemberFn
import io.github.ktor_batterypack.validation.ksp.MemberFnParam

class KspMemberFn(private val fn: KSFunctionDeclaration) : MemberFn {
    override val hasImplementation: Boolean
        get() = !fn.isAbstract

    override val name: String = fn.simpleName.asString()

    override val paramName: String
        get() = fn.parameters[0].name?.asString() ?: ""

    override val paramType: KspCodegenType
        get() = toKspCodegenType(fn.parameters[0].type.resolve())

    override val params: List<MemberFnParam>
        get() = fn.parameters.map {
            MemberFnParam(it.name?.toString()!!, toKspCodegenType(it.type.resolve()))
        }

    override val returnType: CodegenType?
        get() = fn.returnType?.resolve()?.let { toKspCodegenType(it) }

    override val isPublic: Boolean
        get() = fn.modifiers.none {
            it in setOf(
                Modifier.PRIVATE,
                Modifier.PROTECTED,
                Modifier.INTERNAL
            )
        }

    override val annotations: List<CodegenAnnotation>
        get() {
            return fn.annotations.map { KspCodegenAnnotation(it) }.toList()
        }

    override val validationParamType: CodegenType?
        get() = fn.annotations
            .firstOrNull {
                it.annotationType.resolve().declaration
                    .qualifiedName?.asString() == VALIDATION_PARAM_TYPE_FQ_NAME
            }
            ?.arguments
            ?.firstOrNull { it.name?.asString() == "value" }
            ?.value
            ?.let { toKspCodegenType(it as KSType) }

    companion object {
        private const val VALIDATION_PARAM_TYPE_FQ_NAME =
            "io.github.ktor_batterypack.validation.ValidationParamType"
    }

}
