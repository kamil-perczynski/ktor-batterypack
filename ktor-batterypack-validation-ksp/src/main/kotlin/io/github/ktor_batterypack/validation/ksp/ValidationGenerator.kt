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
import io.github.ktor_batterypack.validation.ksp.DefaultCodegenNamingConvention.DEFAULT_CODEGEN_NAMING_CONVENTION
import io.github.ktor_batterypack.validation.symbol.KspCodegenType
import io.github.ktor_batterypack.validation.symbol.KspValidatorInterface
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

        val namingConvention = DEFAULT_CODEGEN_NAMING_CONVENTION
        val codegen = ValidatorCodegen(namingConvention = namingConvention)

        val modelResolver = CodegenModelResolver(
            namingConvention = namingConvention,
            constraintRegistry = constraintRegistry,
            jsonNamingStrategy = JsonNamingStrategy.fromOption(environment.options[JsonNamingStrategy.OPTION])
        )
        val inspector = ValidatorInspector(
            constraintRegistry = constraintRegistry,
            namingConvention = namingConvention,
            strict = environment.options[ValidatorInspector.STRICT_OPTION]?.trim().toBoolean()
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
            generateValidatorClass(inspector, modelResolver, classDecl, jsonMode = false) { model ->
                codegen.generateValidatorClass(model)
            }
        }

        for (classDecl in jsonValidatorClassesSymbols) {
            generateValidatorClass(inspector, modelResolver, classDecl, jsonMode = true) { model ->
                codegen.generateJsonValidatorClass(model)
            }
        }

        generated = true
        return emptyList()
    }

    private fun generateValidatorClass(
        inspector: ValidatorInspector,
        resolver: CodegenModelResolver,
        classDecl: KSClassDeclaration,
        jsonMode: Boolean,
        genFn: (CodegenModel) -> String
    ) {
        val qualifiedName = classDecl.qualifiedName?.asString() ?: return
        environment.logger.info("ValidationGenerator: processing $qualifiedName")

        try {
            val validatorInterface = KspValidatorInterface(classDecl)
            val diagnostics = inspector.inspect(validatorInterface, jsonMode)
            diagnostics.forEach { diagnostic -> report(diagnostic) }
            if (diagnostics.any { diagnostic -> diagnostic.severity == CodegenDiagnostic.Severity.ERROR }) {
                return
            }

            writeValidatorClass(resolver, validatorInterface, classDecl, genFn)
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

    private fun writeValidatorClass(
        resolver: CodegenModelResolver,
        validatorInterface: ValidatorInterface,
        classDecl: KSClassDeclaration,
        genFn: (CodegenModel) -> String
    ) {
        val model = resolver.resolve(validatorInterface)

        val source = genFn(model)

        val sourceFile = classDecl.containingFile ?: return
        val dependencies = Dependencies(aggregating = false, *collectSourceFiles(sourceFile, model))

        environment.codeGenerator
            .createNewFile(
                dependencies = dependencies,
                packageName = model.packageName,
                fileName = model.typeName
            )
            .use { stream -> stream.write(source.toByteArray()) }

        environment.logger.info("Generated ${model.fqTypeName}")
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

private fun collectSourceFiles(interfaceFile: KSFile, model: CodegenModel): Array<KSFile> {
    val files = linkedSetOf(interfaceFile)
    model.methods.forEach { method -> collectSourceFiles(method.codegenType, files) }
    for (method in model.privateMethods) {
        collectSourceFiles(method.codegenType, files)
        method.directProperties.forEach { property -> collectSourceFiles(property.codegenType, files) }
        method.nestedProperties.forEach { property -> collectSourceFiles(property.codegenType, files) }
    }
    return files.toTypedArray()
}

private fun collectSourceFiles(type: CodegenType, files: MutableSet<KSFile>) {
    if (type is KspCodegenType) {
        type.containingFile?.let { file -> files.add(file) }
    }
    type.typeParams.forEach { typeParam -> collectSourceFiles(typeParam, files) }
}