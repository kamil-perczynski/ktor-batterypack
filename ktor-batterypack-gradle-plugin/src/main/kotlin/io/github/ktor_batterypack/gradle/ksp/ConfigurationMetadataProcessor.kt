package io.github.ktor_batterypack.gradle.ksp

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.ArrayNode

class ConfigurationMetadataProcessor(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {

    private var generated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (generated) return emptyList()
        val className = environment.options["configMetadataClass"] ?: return emptyList()
        environment.logger.info("ConfigurationMetadataProcessor: processing $className")

        val ksName = resolver.getKSNameFromString(className)
        val classDecl = resolver.getClassDeclarationByName(ksName)

        if (classDecl == null) {
            environment.logger.error("ConfigurationMetadataProcessor: class $className not found")
            return emptyList()
        }

        val properties = mutableListOf<PropertyMeta>()
        val groups = mutableListOf<GroupMeta>()
        extractProperties(classDecl, resolver, "", className, properties, groups)

        val json = buildMetadataJson(properties, groups)

        val containingFile = classDecl.containingFile
        if (containingFile != null) {
            val fileStream = environment.codeGenerator.createNewFileByPath(
                dependencies = Dependencies(aggregating = false, classDecl.containingFile!!),
                path = "META-INF/spring-configuration-metadata",
                extensionName = "json"
            )

            fileStream.use { stream ->
                stream.write(json.toByteArray())
            }
        } else {
            environment.logger.error("ConfigurationMetadataProcessor: could not determine source file for $className")
        }

        generated = true
        return emptyList()
    }

    private fun extractProperties(
        classDecl: KSClassDeclaration,
        resolver: Resolver,
        keyPrefix: String,
        sourceType: String,
        properties: MutableList<PropertyMeta>,
        groups: MutableList<GroupMeta>
    ) {
        val constructor = classDecl.primaryConstructor ?: return
        for (param in constructor.parameters) {
            val name = param.name?.asString() ?: continue
            val resolvedType = param.type.resolve()
            val qualifiedName = resolvedType.declaration.qualifiedName?.asString()
            val mappedType = resolveTypeWithGenerics(resolvedType)

            val fullName = if (keyPrefix.isEmpty()) name else "$keyPrefix.$name"

            val isBuiltIn = qualifiedName != null &&
                (qualifiedName.startsWith("kotlin.") || qualifiedName.startsWith("java."))

            if (isBuiltIn) {
                properties.add(PropertyMeta(fullName, mappedType, sourceType, param.description()))
                continue
            }

            val nestedClassDecl = resolvedType.declaration as? KSClassDeclaration
            if (nestedClassDecl != null && nestedClassDecl.primaryConstructor != null && nestedClassDecl.classKind == com.google.devtools.ksp.symbol.ClassKind.CLASS) {
                val nestedSourceType = qualifiedName ?: continue
                groups.add(GroupMeta(fullName, nestedSourceType, sourceType))
                extractProperties(
                    nestedClassDecl,
                    resolver,
                    fullName,
                    nestedSourceType,
                    properties,
                    groups
                )
            } else {
                properties.add(PropertyMeta(fullName, mappedType, sourceType, param.description()))
            }
        }
    }

    data class PropertyMeta(
        val name: String,
        val type: String,
        val sourceType: String,
        val description: String? = null
    )
}

data class GroupMeta(val name: String, val type: String, val sourceType: String)

internal fun mapKotlinTypeToJava(kotlinFqcn: String?): String {
    if (kotlinFqcn == null) return "java.lang.Object"
    return when (kotlinFqcn) {
        "kotlin.String" -> "java.lang.String"
        "kotlin.Int" -> "java.lang.Integer"
        "kotlin.Long" -> "java.lang.Long"
        "kotlin.Boolean" -> "java.lang.Boolean"
        "kotlin.Double" -> "java.lang.Double"
        "kotlin.Float" -> "java.lang.Float"
        "kotlin.collections.List" -> "java.util.List"
        "kotlin.collections.MutableList" -> "java.util.List"
        "kotlin.collections.Set" -> "java.util.Set"
        "kotlin.collections.MutableSet" -> "java.util.Set"
        "kotlin.collections.Map" -> "java.util.Map"
        "kotlin.collections.MutableMap" -> "java.util.Map"
        else -> kotlinFqcn
    }
}

internal fun resolveTypeWithGenerics(type: KSType): String {
    val qualifiedName = type.declaration.qualifiedName?.asString()
    val baseType = mapKotlinTypeToJava(qualifiedName)
    val args = type.arguments
    if (args.isEmpty()) return baseType
    val mappedArgs = args.map { arg ->
        val argType = arg.type?.resolve()
        if (argType != null) resolveTypeWithGenerics(argType)
        else "java.lang.Object"
    }
    return "$baseType<${mappedArgs.joinToString(", ")}>"
}

private val mapper = ObjectMapper()

private fun KSValueParameter.description(): String? {
    val descAnnotation = annotations.firstOrNull {
        it.annotationType.resolve().declaration.qualifiedName?.asString() == DESCRIPTION_FQCN
    } ?: return null
    return descAnnotation.arguments.firstOrNull()?.value as? String
}

private const val DESCRIPTION_FQCN = "com.fasterxml.jackson.annotation.JsonPropertyDescription"

internal fun buildMetadataJson(
    properties: List<ConfigurationMetadataProcessor.PropertyMeta>,
    groups: List<GroupMeta>
): String {
    val root: ObjectNode = mapper.createObjectNode()

    val propsArray: ArrayNode = mapper.createArrayNode()
    properties.forEach { prop ->
        val entry: ObjectNode = mapper.createObjectNode()
        entry.put("name", prop.name)
        entry.put("type", prop.type)
        entry.put("sourceType", prop.sourceType)
        if (prop.description != null) {
            entry.put("description", prop.description)
        }
        propsArray.add(entry)
    }
    root.set("properties", propsArray)

    if (groups.isNotEmpty()) {
        val groupsArray: ArrayNode = mapper.createArrayNode()
        groups.forEach { group ->
            val entry: ObjectNode = mapper.createObjectNode()
            entry.put("name", group.name)
            entry.put("type", group.type)
            entry.put("sourceType", group.sourceType)
            groupsArray.add(entry)
        }
        root.set("groups", groupsArray)
    }

    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root)
}

class ConfigurationMetadataProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ConfigurationMetadataProcessor(environment)
    }
}
