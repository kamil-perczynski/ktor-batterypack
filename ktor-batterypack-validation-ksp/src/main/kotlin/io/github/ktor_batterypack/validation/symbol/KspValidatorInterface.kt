package io.github.ktor_batterypack.validation.symbol

import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toClassName
import io.github.ktor_batterypack.validation.ksp.MemberFn
import io.github.ktor_batterypack.validation.ksp.ValidatorInterface
import io.github.ktor_batterypack.validation.ksp.ValidatorProperty

class KspValidatorInterface(private val classDecl: KSClassDeclaration) : ValidatorInterface {

    override val name: String = classDecl.simpleName.asString()

    override val fqName: String = classDecl.qualifiedName?.asString() ?: error("No qualified name")

    override val className: ClassName
        get() = classDecl.toClassName()

    override val symbol: Any?
        get() = classDecl

    override val memberFunctions: List<MemberFn>
        get() = classDecl.declarations
            .filterIsInstance<KSFunctionDeclaration>()
            .map { KspMemberFn(it) }
            .toList()

    override val abstractProperties: List<ValidatorProperty>
        get() = classDecl.getAllProperties()
            .filter { property -> property.isAbstract() }
            .map { property ->
                ValidatorProperty(
                    name = property.simpleName.asString(),
                    type = toKspCodegenType(property.type.resolve()),
                    isMutable = property.isMutable,
                    symbol = property
                )
            }
            .toList()
}
