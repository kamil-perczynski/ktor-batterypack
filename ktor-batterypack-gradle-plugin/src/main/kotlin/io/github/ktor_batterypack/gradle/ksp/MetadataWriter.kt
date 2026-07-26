package io.github.ktor_batterypack.gradle.ksp

import tools.jackson.databind.json.JsonMapper
import tools.jackson.dataformat.yaml.YAMLMapper

object MetadataWriter {

    private val jsonMapper: JsonMapper = JsonMapper.builder().build()
    private val yamlMapper: YAMLMapper = YAMLMapper.builder().build()

    fun writeJson(metadata: SpringConfigMetadata): String =
        jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(metadata)

    fun writeYaml(metadata: SpringConfigMetadata): String {
        val schema = buildJsonSchema(metadata)
        return yamlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema)
    }

    private fun buildJsonSchema(metadata: SpringConfigMetadata): Map<String, Any> {
        val root = linkedMapOf<String, Any>(
            "\$schema" to "https://json-schema.org/draft/2020-12/schema",
            "type" to "object",
            "properties" to linkedMapOf<String, Any>()
        )

        val groupTypeByName = metadata.groups.associate { it.name to it.type }

        @Suppress("UNCHECKED_CAST")
        val rootProps = root["properties"] as MutableMap<String, Any>

        val propertyNodes = metadata.properties.sortedBy { it.name }

        propertyNodes.forEach { prop ->
            val parts = prop.name.split(".")
            var current: MutableMap<String, Any> = rootProps
            for (i in 0 until parts.size - 1) {
                val part = parts[i]
                @Suppress("UNCHECKED_CAST")
                val child = current.getOrPut(part) {
                    buildObjectSchema(parts.take(i + 1).joinToString("."), groupTypeByName)
                } as MutableMap<String, Any>
                @Suppress("UNCHECKED_CAST")
                current = child["properties"] as MutableMap<String, Any>
            }

            val leafName = parts.last()
            current[leafName] = schemaLeaf(prop)
        }

        metadata.groups.forEach { group ->
            val parts = group.name.split(".")
            var current: MutableMap<String, Any> = rootProps
            for (i in 0 until parts.size - 1) {
                val part = parts[i]
                @Suppress("UNCHECKED_CAST")
                val child = current.getOrPut(part) {
                    buildObjectSchema(parts.take(i + 1).joinToString("."), groupTypeByName)
                } as MutableMap<String, Any>
                @Suppress("UNCHECKED_CAST")
                current = child["properties"] as MutableMap<String, Any>
            }
            val groupName = parts.last()
            current.getOrPut(groupName) {
                buildObjectSchema(group.name, groupTypeByName)
            }
        }

        return root
    }

    private fun buildObjectSchema(name: String, groupTypeByName: Map<String, String>): LinkedHashMap<String, Any> {
        val schema = linkedMapOf<String, Any>(
            "type" to "object",
            "properties" to linkedMapOf<String, Any>()
        )
        val fullType = groupTypeByName[name]
        if (fullType != null) {
            schema["description"] = fullType
        }
        return schema
    }

    private fun schemaLeaf(prop: ConfigProperty): Map<String, Any> {
        val result = linkedMapOf<String, Any>()
        val schemaType = mapTypeToJsonSchemaType(prop.type)
        result["type"] = schemaType

        if (prop.description != null) {
            result["description"] = prop.description
        }

        if (prop.type.startsWith("java.util.")) {
            val itemType = extractGenericArg(prop.type)
            if (itemType != null) {
                result["items"] = mapOf("type" to mapTypeToJsonSchemaType(itemType))
            }
        }

        return result
    }

    private fun mapTypeToJsonSchemaType(javaType: String): String = when {
        javaType == "java.lang.String" -> "string"
        javaType == "java.lang.Integer" -> "integer"
        javaType == "java.lang.Long" -> "integer"
        javaType == "java.lang.Boolean" -> "boolean"
        javaType == "java.lang.Double" -> "number"
        javaType == "java.lang.Float" -> "number"
        javaType.startsWith("java.util.List") -> "array"
        javaType.startsWith("java.util.Set") -> "array"
        javaType.startsWith("java.util.Map") -> "object"
        else -> "object"
    }

    private fun extractGenericArg(javaType: String): String? {
        val start = javaType.indexOf('<')
        val end = javaType.lastIndexOf('>')
        if (start == -1 || end == -1) return null
        return javaType.substring(start + 1, end).trim()
    }
}
