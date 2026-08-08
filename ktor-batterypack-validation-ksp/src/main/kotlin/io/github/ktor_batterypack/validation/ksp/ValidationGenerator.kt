package io.github.ktor_batterypack.validation.ksp

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.github.ktor_batterypack.validation.symbol.KspValidatorInterface

class ValidationGenerator(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {

    private var generated = false
    private val codegen = ValidatorCodegen()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (generated) return emptyList()

        val annotated = resolver.getSymbolsWithAnnotation("io.github.ktor_batterypack.annotation.Validator")
            .filterIsInstance<KSClassDeclaration>()
            .toList()

        val deferred = mutableListOf<KSAnnotated>()

        for (classDecl in annotated) {
            val qualifiedName = classDecl.qualifiedName?.asString() ?: continue
            environment.logger.warn("ValidationGenerator: processing $qualifiedName")

            val model = V2CodegenModelResolverImpl().resolve(KspValidatorInterface(classDecl))
            val source = codegen.generateValidatorClass(model)

            val sourceFile = classDecl.containingFile ?: continue
            val dependencies = Dependencies(aggregating = false, sourceFile)

            environment.codeGenerator.createNewFile(
                dependencies = dependencies,
                packageName = model.packageName,
                fileName = model.typeName
            ).use { stream ->
                stream.write(source.toByteArray())
            }

            environment.logger.warn("Generated ${model.fqTypeName}")
        }

        generated = true
        return deferred
    }
}

class ValidationGeneratorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ValidationGenerator(environment)
    }
}
