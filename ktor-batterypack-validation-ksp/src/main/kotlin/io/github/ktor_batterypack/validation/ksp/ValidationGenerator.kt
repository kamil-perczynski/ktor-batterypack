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

        val validatorClassesSymbols =
            resolver.getSymbolsWithAnnotation("io.github.ktor_batterypack.annotation.Validator")
                .filterIsInstance<KSClassDeclaration>()
                .toList()

        val jsonValidatorClassesSymbols =
            resolver.getSymbolsWithAnnotation("io.github.ktor_batterypack.annotation.JsonValidator")
                .filterIsInstance<KSClassDeclaration>()
                .toList()

        val deferred = mutableListOf<KSAnnotated>()

        for (classDecl in validatorClassesSymbols) {
            generateValidatorClass(classDecl) { model ->
                codegen.generateValidatorClass(model)
            }
        }
        for (classDecl in jsonValidatorClassesSymbols) {
            generateValidatorClass(classDecl) { model ->
                codegen.generateJsonValidatorClass(model)
            }
        }

        generated = true
        return deferred
    }

    private fun generateValidatorClass(
        classDecl: KSClassDeclaration,
        genFn: (CodegenModel) -> String
    ) {
        val qualifiedName = classDecl.qualifiedName?.asString() ?: return
        environment.logger.warn("ValidationGenerator: processing $qualifiedName")

        val resolver = CodegenModelResolver(
            namingConvention = DefaultCodegenNamingConvention.DEFAULT_CODEGEN_NAMING_CONVENTION
        )

        val model = resolver.resolve(KspValidatorInterface(classDecl))

        val source = genFn(model)

        val sourceFile = classDecl.containingFile ?: return
        val dependencies = Dependencies(aggregating = false, sourceFile)

        environment.codeGenerator
            .createNewFile(
                dependencies = dependencies,
                packageName = model.packageName,
                fileName = model.typeName
            )
            .use { stream -> stream.write(source.toByteArray()) }

        environment.logger.warn("Generated ${model.fqTypeName}")
    }
}

class ValidationGeneratorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ValidationGenerator(environment)
    }
}
