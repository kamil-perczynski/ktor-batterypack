package io.github.ktor_batterypack.validation.codegen

import org.junit.jupiter.api.Test
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.module.kotlin.KotlinModule

class ConstraintsDescriptorTest {

    @Test
    fun name() {
        val yamlBytes = ConstraintsDescriptor::class.java
            .getResourceAsStream("/constraints/jakarta-contraints.yaml")!!
            .readAllBytes()

        val yamlMapper = YAMLMapper.builder()
            .addModule(KotlinModule.Builder().build())
            .build()

        val descriptor = yamlMapper.readValue(yamlBytes, ConstraintsDescriptor::class.java)

        val constraints = descriptor.constraints.map {
            it.copy(
                callTpl = it.callTpl ?: descriptor.defaults.callTpl,
                jsonCallTpl = it.jsonCallTpl ?: descriptor.defaults.jsonCallTpl,
            )
        }
        println(constraints)
    }
}