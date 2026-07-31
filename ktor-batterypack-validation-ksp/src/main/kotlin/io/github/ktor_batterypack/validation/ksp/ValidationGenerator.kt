package io.github.ktor_batterypack.validation.ksp

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

class ValidationGenerator(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {

    private var generated = false
    private val handlebars = Handlebars(ClassPathTemplateLoader("/"))
    private val template = handlebars.compile("validation-impl")

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (generated) {
            return emptyList()
        }

        val annotated = resolver.getSymbolsWithAnnotation("io.github.ktor_batterypack.annotation.Validator")
            .filterIsInstance<KSClassDeclaration>()
            .toList()

        val deferred = mutableListOf<KSAnnotated>()

        for (classDecl in annotated) {
            val qualifiedName = classDecl.qualifiedName?.asString() ?: continue
            environment.logger.warn("ValidationGenerator: processing $qualifiedName")

            val model = buildModel(classDecl, resolver)
            val source = template.apply(model)

            val sourceFile = classDecl.containingFile ?: continue
            val dependencies = Dependencies(aggregating = false, sourceFile)

            environment.codeGenerator.createNewFile(
                dependencies = dependencies,
                packageName = model.packageName,
                fileName = model.implSimpleName
            ).use { stream ->
                stream.write(source.toByteArray())
            }

            println("Generated ${model.packageName}.${model.implSimpleName}")
            println(source)
        }

        generated = true
        return deferred
    }

    private fun buildModel(interfaceDecl: KSClassDeclaration, resolver: Resolver): ValidationModel {
        val packageName = interfaceDecl.packageName.asString()
        val interfaceSimpleName = interfaceDecl.simpleName.asString()
        val implSimpleName = interfaceSimpleName + "Impl"

        val imports = mutableListOf<String>()
        val methods = mutableListOf<MethodModel>()

        for (method in interfaceDecl.getAllFunctions()) {
            val methodName = method.simpleName.asString()
            if (methodName == "equals" || methodName == "hashCode" || methodName == "toString") {
                continue
            }

            val param = method.parameters.first()
            val paramName = param.name?.asString() ?: continue
            val paramType = param.type.resolve()
            val paramTypeDecl = paramType.declaration as? KSClassDeclaration ?: continue
            val dtoSimpleName = paramTypeDecl.simpleName.asString()
            val dtoQualifiedName = paramTypeDecl.qualifiedName?.asString() ?: continue

            imports.add(dtoQualifiedName)
            val properties = extractProperties(paramTypeDecl, resolver, "")

            methods.add(
                MethodModel(
                    name = methodName,
                    paramName = paramName,
                    dtoSimpleName = dtoSimpleName,
                    properties = properties
                )
            )
        }

        imports.sort()

        return ValidationModel(
            packageName = packageName,
            interfaceSimpleName = interfaceSimpleName,
            implSimpleName = implSimpleName,
            imports = imports,
            methods = methods
        )
    }

    private fun extractProperties(
        classDecl: KSClassDeclaration,
        resolver: Resolver,
        prefix: String
    ): List<PropertyModel> {
        val constructor = classDecl.primaryConstructor ?: return emptyList()
        return constructor.parameters.mapNotNull { param ->
            val name = param.name?.asString() ?: return@mapNotNull null
            val type = param.type.resolve()
            val typeName = type.declaration.qualifiedName?.asString() ?: return@mapNotNull null
            val isNullable = type.isMarkedNullable
            val accessor = if (prefix.isEmpty()) name else "$prefix.$name"

            val nestedDecl = type.declaration as? KSClassDeclaration
            val nested = if (nestedDecl != null && nestedDecl.primaryConstructor != null &&
                !typeName.startsWith("kotlin.") && !typeName.startsWith("java.")
            ) {
                extractProperties(nestedDecl, resolver, accessor)
            } else {
                emptyList()
            }

            PropertyModel(
                name = name,
                typeName = typeName,
                isString = typeName == "kotlin.String",
                isNumeric = typeName in NUMERIC_TYPES,
                isBoolean = typeName == "kotlin.Boolean",
                isNullable = isNullable,
                accessor = accessor,
                nested = nested
            )
        }
    }

    companion object {
        private val NUMERIC_TYPES = setOf(
            "kotlin.Int",
            "kotlin.Long",
            "kotlin.Double",
            "kotlin.Float",
            "kotlin.Short",
            "kotlin.Byte"
        )
    }
}

class ValidationGeneratorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ValidationGenerator(environment)
    }
}