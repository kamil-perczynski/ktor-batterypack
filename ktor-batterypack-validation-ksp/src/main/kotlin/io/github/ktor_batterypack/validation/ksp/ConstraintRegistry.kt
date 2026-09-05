package io.github.ktor_batterypack.validation.ksp

import io.github.ktor_batterypack.validation.codegen.ConstraintsDescriptor
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.module.kotlin.KotlinModule

class ConstraintRegistry {

    private val constraintDescriptors: List<ConstraintsDescriptor.Constraint>

    init {
        val urls = javaClass.getResource("/constraints/jakarta-contraints.yaml")

        val res = javaClass.getResourceAsStream("/constraints/jakarta-contraints.yaml")
            ?: throw IllegalStateException("Could not load jakarta-contraints.yaml resource, $urls")

        val yamlBytes = try {
            res.bufferedReader().readAllAsString()
        } catch (e: Exception) {
            throw IllegalStateException("Could not read the file: ${e.message}", e)
        }

        val yamlMapper = YAMLMapper.builder()
            .build()

        val descriptor = yamlMapper.readValue(yamlBytes, ConstraintsDescriptor::class.java)

        this.constraintDescriptors = descriptor.constraints.map {
            it.copy(
                callTpl = it.callTpl ?: descriptor.defaults.callTpl,
                jsonCallTpl = it.jsonCallTpl ?: descriptor.defaults.jsonCallTpl,
            )
        }
    }


    fun findItemConstraints(codegenType: CodegenType): Map<String, ConstraintMethod> {
        if (codegenType.isCollection) {
            val type = codegenType.typeParams[0]
            return toConstraints(type, type.annotations)
        }

        if (codegenType.isMap) {
            val type = codegenType.typeParams[1]
            return toConstraints(type, type.annotations)
        }

        return toConstraints(codegenType, codegenType.annotations)
    }

    fun toConstraints(
        codegenType: CodegenType,
        annotations: List<CodegenAnnotation>
    ): Map<String, ConstraintMethod> {
        if (annotations.isEmpty()) {
            return emptyMap()
        }

        val boo = annotations
            .mapNotNull { constraint ->
                val descriptor = constraintDescriptors.find { descriptor ->
                    constraint.fqName == descriptor.annotation
                }

                if (descriptor == null) {
                    return@mapNotNull null
                }

                constraint to descriptor
            }
            .associate { (constraint, descriptor) ->
                constraint.name to ConstraintMethod(
                    "check${constraint.name}",
                    constraint.args,
                    descriptor,
                )
            }

        return boo
    }
}
