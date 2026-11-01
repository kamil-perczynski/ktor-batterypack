package io.github.ktor_batterypack.validation.ksp

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import io.github.ktor_batterypack.validation.codegen.ConstraintDescriptionSource
import io.github.ktor_batterypack.validation.codegen.ConstraintRegistry
import io.github.ktor_batterypack.validation.codegen.JakartaConstraintsSource
import io.github.ktor_batterypack.validation.codegen.YamlFileConstraintsSource
import io.github.ktor_batterypack.validation.codegen.emit.ValidatorEmitter
import io.github.ktor_batterypack.validation.symbol.KspCodegenType
import io.github.ktor_batterypack.validation.symbol.KspValidatorInterface
import com.squareup.kotlinpoet.ksp.writeTo
import java.nio.file.Files
import java.nio.file.Path
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

        val emitter = ValidatorEmitter()
        val modelResolver = CodegenModelResolver(
            constraintRegistry = constraintRegistry,
            jsonNamingStrategy = JsonNamingStrategy.fromOption(environment.options[JsonNamingStrategy.OPTION]),
            strict = environment.options[CodegenModelResolver.STRICT_OPTION]?.trim().toBoolean()
        )

        val validatorClassesSymbols =
            resolver.getSymbolsWithAnnotation("io.github.ktor_batterypack.annotation.Validator")
                .filterIsInstance<KSClassDeclaration>()
                .toList()

        val jsonValidatorClassesSymbols =
            resolver.getSymbolsWithAnnotation("io.github.ktor_batterypack.annotation.JsonValidator")
                .filterIsInstance<KSClassDeclaration>()
                .toList()

        for (classDecl in validatorClassesSymbols) {
            generateValidatorClass(modelResolver, emitter, classDecl, CodegenMode.TYPED)
        }

        for (classDecl in jsonValidatorClassesSymbols) {
            generateValidatorClass(modelResolver, emitter, classDecl, CodegenMode.JSON)
        }

        generated = true
        return emptyList()
    }

    private fun generateValidatorClass(
        resolver: CodegenModelResolver,
        emitter: ValidatorEmitter,
        classDecl: KSClassDeclaration,
        mode: CodegenMode
    ) {
        val qualifiedName = classDecl.qualifiedName?.asString() ?: return
        environment.logger.info("ValidationGenerator: processing $qualifiedName")

        try {
            val resolved = resolver.resolve(KspValidatorInterface(classDecl), mode)
            resolved.diagnostics.forEach { diagnostic -> report(diagnostic) }
            val model = resolved.model ?: return

            writeValidatorClass(emitter, model, classDecl)
        } catch (e: Exception) {
            environment.logger.error("Could not generate a validator for $qualifiedName: $e", classDecl)
        }
    }

    private fun report(diagnostic: CodegenDiagnostic) {
        val symbol = diagnostic.symbol as? KSNode
        when (diagnostic.severity) {
            CodegenDiagnostic.Severity.ERROR -> environment.logger.error(diagnostic.message, symbol)
            CodegenDiagnostic.Severity.WARNING -> environment.logger.warn(diagnostic.message, symbol)
        }
    }

    private fun writeValidatorClass(emitter: ValidatorEmitter, model: ValidatorModel, classDecl: KSClassDeclaration) {
        val sourceFile = classDecl.containingFile ?: return
        val dependencies = Dependencies(aggregating = false, *collectSourceFiles(sourceFile, model))

        emitter.emit(model).writeTo(environment.codeGenerator, dependencies)

        environment.logger.info("Generated ${model.implClassName.canonicalName}")
    }
}

class ValidationGeneratorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ValidationGenerator(environment)
    }
}

private fun loadConstraintDescriptorDir(constraintDescriptorDir: String?): List<ConstraintDescriptionSource> {
    if (constraintDescriptorDir == null) return emptyList()

    return Files.list(Paths.get(constraintDescriptorDir)).use { paths ->
        paths
            .filter { path -> Files.isRegularFile(path) && isYamlFile(path) }
            .sorted()
            .map { path -> YamlFileConstraintsSource(path) }
            .toList()
    }
}

private fun isYamlFile(path: Path): Boolean {
    val fileName = path.fileName.toString()
    return fileName.endsWith(".yaml") || fileName.endsWith(".yml")
}

private fun collectSourceFiles(interfaceFile: KSFile, model: ValidatorModel): Array<KSFile> {
    val files = linkedSetOf(interfaceFile)
    model.entryMethods.forEach { method -> collectSourceFiles(method.value.type, files) }
    for (method in model.typeMethods) {
        collectSourceFiles(method.type, files)
        method.properties.forEach { property -> collectSourceFiles(property.value.type, files) }
    }
    return files.toTypedArray()
}

private fun collectSourceFiles(type: CodegenType, files: MutableSet<KSFile>) {
    if (type is KspCodegenType) {
        type.containingFile?.let { file -> files.add(file) }
    }
    type.typeParams.forEach { typeParam -> collectSourceFiles(typeParam, files) }
}