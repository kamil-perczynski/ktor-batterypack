package io.github.ktor_batterypack.validation.symbol

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import io.github.ktor_batterypack.validation.ksp.CodegenType
import io.github.ktor_batterypack.validation.ksp.DeclaredConstraint
import io.github.ktor_batterypack.validation.ksp.DeclaredMember

class KspDeclaredMember(private val prop: KSPropertyDeclaration) : DeclaredMember {

    override val constraints: List<DeclaredConstraint>
        get() {
            return prop.annotations.map { KspDeclaredConstraint(it) }.toList()
        }

    override val name: String
        get() = prop.simpleName.asString()

    override val type: CodegenType
        get() = toKspCodegenType(prop.type.resolve())

    override val itemType: CodegenType?
        get() {
            if (type.isCollection) {
                val typeArgs = prop.type.resolve().arguments
                return toKspCodegenType(typeArgs.first().type!!.resolve())
            }
            return null
        }
}
