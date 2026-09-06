package io.github.ktor_batterypack.validation.codegen

import tools.jackson.dataformat.yaml.YAMLMapper

class JakartaConstraintsSource : ConstraintDescriptionSource {

    override fun loadConstraintDescriptor(yamlMapper: YAMLMapper): ConstraintsDescriptor {
        val urls = javaClass.getResource("/constraints/jakarta-contraints.yaml")

        val res = javaClass.getResourceAsStream("/constraints/jakarta-contraints.yaml")
            ?: throw IllegalStateException("Could not load jakarta-contraints.yaml resource, $urls")

        return toConstraintDescriptor(res, yamlMapper)
    }

}