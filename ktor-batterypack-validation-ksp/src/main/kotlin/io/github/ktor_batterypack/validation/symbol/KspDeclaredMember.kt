package io.github.ktor_batterypack.validation.symbol

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import io.github.ktor_batterypack.validation.ksp.CodegenType
import io.github.ktor_batterypack.validation.ksp.CodegenAnnotation
import io.github.ktor_batterypack.validation.ksp.DeclaredMember
import io.github.ktor_batterypack.validation.ksp.mergeAnnotationSites

class KspDeclaredMember(
    private val prop: KSPropertyDeclaration,
    private val constructorParameter: KSValueParameter?,
    private val hasPrimaryConstructor: Boolean
) : DeclaredMember {

    override val annotations: List<CodegenAnnotation> by lazy {
        val getterAnnotations = prop.getter?.annotations ?: emptySequence()
        val parameterAnnotations = constructorParameter?.annotations ?: emptySequence()
        val overriddenAnnotations = generateSequence(prop.findOverridee()) { overridden -> overridden.findOverridee() }
            .flatMap { overridden -> sequenceOf(overridden.annotations, overridden.getter?.annotations ?: emptySequence()) }
            .toList()

        mergeAnnotationSites(
            (listOf(prop.annotations, getterAnnotations, parameterAnnotations) + overriddenAnnotations).map { site ->
                site.map { annotation -> KspCodegenAnnotation(annotation) }.toList()
            }
        )
    }

    override val name: String
        get() = prop.simpleName.asString()

    override val type: CodegenType
        get() = toKspCodegenType(prop.type.resolve())

    override val symbol: Any?
        get() = prop

    override val hasDefault: Boolean
        get() {
            if (constructorParameter != null) {
                return constructorParameter.hasDefault
            }
            return hasPrimaryConstructor
        }

}
