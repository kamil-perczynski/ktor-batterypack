package io.github.ktor_batterypack.validation.symbol

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import io.github.ktor_batterypack.validation.ksp.CodegenType
import io.github.ktor_batterypack.validation.ksp.CodegenAnnotation
import io.github.ktor_batterypack.validation.ksp.DeclaredMember

class KspDeclaredMember(private val prop: KSPropertyDeclaration) : DeclaredMember {

    override val annotations: List<CodegenAnnotation>
        get() = prop.annotations.map { KspCodegenAnnotation(it) }.toList()

    override val name: String
        get() = prop.simpleName.asString()

    override val type: CodegenType
        get() = toKspCodegenType(prop.type.resolve())

}
