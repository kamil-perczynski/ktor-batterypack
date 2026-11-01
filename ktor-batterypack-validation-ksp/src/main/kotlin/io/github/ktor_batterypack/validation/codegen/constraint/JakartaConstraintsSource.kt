package io.github.ktor_batterypack.validation.codegen.constraint

import tools.jackson.dataformat.yaml.YAMLMapper

class JakartaConstraintsSource : ConstraintDescriptionSource {

    override fun loadConstraintDescriptor(yamlMapper: YAMLMapper): ConstraintsDescriptor {
        val urls = javaClass.getResource("/constraints/jakarta-constraints.yaml")

        val res = javaClass.getResourceAsStream("/constraints/jakarta-constraints.yaml")
            ?: throw IllegalStateException("Could not load jakarta-constraints.yaml resource, $urls")

        return toConstraintDescriptor(res, yamlMapper)
    }

}