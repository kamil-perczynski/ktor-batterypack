package io.github.ktor_batterypack.validation.ksp

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.github.ktor_batterypack.validation.codegen.ConstraintDescriptionSource
import io.github.ktor_batterypack.validation.codegen.ConstraintRegistry
import io.github.ktor_batterypack.validation.codegen.JakartaConstraintsSource
import io.github.ktor_batterypack.validation.codegen.YamlFileConstraintsSource
import io.github.ktor_batterypack.validation.ksp.DefaultCodegenNamingConvention.DEFAULT_CODEGEN_NAMING_CONVENTION
import io.github.ktor_batterypack.validation.symbol.KspValidatorInterface
import java.nio.file.Files
import java.nio.file.Paths

class ValidationGenerator(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {

    private var generated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (generated) {
            return emptyList()
        }

        val constraintDescriptorDir = environment.options["ktor.validation.constraint.dir"]
        val constraintRegistry = ConstraintRegistry(
            sources = loadConstraintDescriptorDir(constraintDescriptorDir)
                .plus(listOf(JakartaConstraintsSource()))
        )

        val namingConvention = DEFAULT_CODEGEN_NAMING_CONVENTION
        val codegen = ValidatorCodegen(namingConvention = namingConvention)

        val modelResolver = CodegenModelResolver(
            namingConvention = namingConvention,
            constraintRegistry = constraintRegistry
        )

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
            generateValidatorClass(modelResolver, classDecl) { model ->
                codegen.generateValidatorClass(model)
            }
        }

        for (classDecl in jsonValidatorClassesSymbols) {
            generateValidatorClass(modelResolver, classDecl) { model ->
                codegen.generateJsonValidatorClass(model)
            }
        }

        generated = true
        return deferred
    }

    private fun generateValidatorClass(
        resolver: CodegenModelResolver,
        classDecl: KSClassDeclaration,
        genFn: (CodegenModel) -> String
    ) {
        val qualifiedName = classDecl.qualifiedName?.asString() ?: return
        environment.logger.info("ValidationGenerator: processing $qualifiedName")

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

private fun loadConstraintDescriptorDir(constraintDescriptorDir: String?): List<ConstraintDescriptionSource> {
    if (constraintDescriptorDir == null) return emptyList()

    return Files.list(Paths.get(constraintDescriptorDir))
        .map { YamlFileConstraintsSource(it) }
        .toList()
}