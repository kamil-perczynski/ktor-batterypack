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
import com.google.devtools.ksp.validate
import io.github.ktor_batterypack.validation.codegen.constraint.ConstraintDescriptionSource
import io.github.ktor_batterypack.validation.codegen.constraint.ConstraintRegistry
import io.github.ktor_batterypack.validation.codegen.constraint.JakartaConstraintsSource
import io.github.ktor_batterypack.validation.codegen.constraint.YamlFileConstraintsSource
import io.github.ktor_batterypack.validation.codegen.emit.ValidatorEmitter
import io.github.ktor_batterypack.validation.symbol.KspCodegenType
import io.github.ktor_batterypack.validation.symbol.KspValidatorInterface
import com.squareup.kotlinpoet.ksp.writeTo
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ValidationGenerator(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {

    private val emitter = ValidatorEmitter()

    private val modelResolver by lazy {
        val constraintDescriptorDir = environment.options["ktor.validation.constraint.dir"]
        CodegenModelResolver(
            constraintRegistry = ConstraintRegistry(
                sources = loadConstraintDescriptorDir(constraintDescriptorDir)
                    .plus(listOf(JakartaConstraintsSource()))
            ),
            jsonNamingStrategy = JsonNamingStrategy.fromOption(environment.options[JsonNamingStrategy.OPTION]),
            strict = environment.options[CodegenModelResolver.STRICT_OPTION]?.trim().toBoolean()
        )
    }

    private var deferredValidators = emptyList<DeferredValidator>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val deferred = mutableListOf<DeferredValidator>()

        for (classDecl in validatorSymbols(resolver, "io.github.ktor_batterypack.annotation.Validator")) {
            generateValidatorClass(classDecl, CodegenMode.TYPED)?.let { validator -> deferred.add(validator) }
        }

        for (classDecl in validatorSymbols(resolver, "io.github.ktor_batterypack.annotation.JsonValidator")) {
            generateValidatorClass(classDecl, CodegenMode.JSON)?.let { validator -> deferred.add(validator) }
        }

        deferredValidators = deferred
        return deferred.map { validator -> validator.classDecl }
    }

    override fun finish() {
        for (validator in deferredValidators) {
            validator.diagnostics.forEach { diagnostic -> report(diagnostic) }
            if (validator.diagnostics.none { diagnostic -> diagnostic.severity == CodegenDiagnostic.Severity.ERROR }) {
                environment.logger.error(
                    "Could not generate a validator for ${validator.classDecl.qualifiedName?.asString()}: " +
                        "some of its types could not be resolved",
                    validator.classDecl
                )
            }
        }
        deferredValidators = emptyList()
    }

    private fun validatorSymbols(resolver: Resolver, annotationName: String): List<KSClassDeclaration> {
        return resolver.getSymbolsWithAnnotation(annotationName)
            .filterIsInstance<KSClassDeclaration>()
            .toList()
    }

    private fun generateValidatorClass(classDecl: KSClassDeclaration, mode: CodegenMode): DeferredValidator? {
        val qualifiedName = classDecl.qualifiedName?.asString() ?: return null
        environment.logger.info("ValidationGenerator: processing $qualifiedName")

        if (!classDecl.validate()) {
            return defer(classDecl, emptyList())
        }

        try {
            val resolved = modelResolver.resolve(KspValidatorInterface(classDecl), mode)
            if (resolved.hasUnresolvedTypes) {
                return defer(classDecl, resolved.diagnostics)
            }

            resolved.diagnostics.forEach { diagnostic -> report(diagnostic) }
            val model = resolved.model ?: return null

            writeValidatorClass(model, classDecl)
        } catch (e: Exception) {
            environment.logger.error("Could not generate a validator for $qualifiedName: $e", classDecl)
        }
        return null
    }

    private fun defer(classDecl: KSClassDeclaration, diagnostics: List<CodegenDiagnostic>): DeferredValidator {
        environment.logger.info("ValidationGenerator: deferring ${classDecl.qualifiedName?.asString()} until its types are resolved")
        return DeferredValidator(classDecl, diagnostics)
    }

    private fun report(diagnostic: CodegenDiagnostic) {
        val symbol = diagnostic.symbol as? KSNode
        when (diagnostic.severity) {
            CodegenDiagnostic.Severity.ERROR -> environment.logger.error(diagnostic.message, symbol)
            CodegenDiagnostic.Severity.WARNING -> environment.logger.warn(diagnostic.message, symbol)
        }
    }

    private fun writeValidatorClass(model: ValidatorModel, classDecl: KSClassDeclaration) {
        val sourceFile = classDecl.containingFile ?: return
        val aggregating = model.typeMethods.any { method -> method.dispatch != null }
        val dependencies = Dependencies(aggregating, *collectSourceFiles(sourceFile, model))

        emitter.emit(model).writeTo(environment.codeGenerator, dependencies)

        environment.logger.info("Generated ${model.implClassName.canonicalName}")
    }

    private data class DeferredValidator(
        val classDecl: KSClassDeclaration,
        val diagnostics: List<CodegenDiagnostic>
    )
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