package io.github.ktor_batterypack.validation.codegen

import tools.jackson.dataformat.yaml.YAMLMapper
import java.io.InputStream

fun interface ConstraintDescriptionSource {
    fun loadConstraintDescriptor(yamlMapper: YAMLMapper): ConstraintsDescriptor
}

internal fun toConstraintDescriptor(input: InputStream, yaml: YAMLMapper): ConstraintsDescriptor {
    val yamlBytes = try {
        input.bufferedReader().readAllAsString()
    } catch (e: Exception) {
        throw IllegalStateException("Could not read the file: ${e.message}", e)
    }

    return try {
        yaml.readValue(yamlBytes, ConstraintsDescriptor::class.java)
    } catch (e: Exception) {
        throw IllegalStateException("Could not deserialize: ${e.message}", e)
    }
}
