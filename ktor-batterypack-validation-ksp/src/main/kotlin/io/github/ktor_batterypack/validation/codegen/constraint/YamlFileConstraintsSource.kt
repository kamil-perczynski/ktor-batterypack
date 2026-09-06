package io.github.ktor_batterypack.validation.codegen

import tools.jackson.dataformat.yaml.YAMLMapper
import java.nio.file.Files
import java.nio.file.Path

class YamlFileConstraintsSource(private val path: Path) : ConstraintDescriptionSource {

    override fun loadConstraintDescriptor(yamlMapper: YAMLMapper): ConstraintsDescriptor {
        if (!Files.exists(path)) {
            throw IllegalStateException("Path does not exist: $path")
        }

        val res = Files.newInputStream(path)

        return toConstraintDescriptor(res, yamlMapper)
    }

}