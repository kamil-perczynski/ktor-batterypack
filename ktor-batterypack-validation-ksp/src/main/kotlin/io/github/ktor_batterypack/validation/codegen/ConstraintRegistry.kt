package io.github.ktor_batterypack.validation.codegen

import io.github.ktor_batterypack.validation.ksp.CodegenAnnotation
import io.github.ktor_batterypack.validation.ksp.CodegenType
import io.github.ktor_batterypack.validation.ksp.ConstraintMethod
import tools.jackson.dataformat.yaml.YAMLMapper

class ConstraintRegistry(sources: List<ConstraintDescriptionSource>) {

    companion object {
        fun default(): ConstraintRegistry {
            return ConstraintRegistry(listOf(JakartaConstraintsSource()))
        }
    }

    private val constraintDescriptors: List<ConstraintsDescriptor>

    init {
        val yamlMapper = YAMLMapper.builder()
            .build()

        constraintDescriptors = sources
            .map { it.loadConstraintDescriptor(yamlMapper) }
            .map { descriptor ->
                val constraints = descriptor.constraints.map {
                    it.copy(
                        callTpl = it.callTpl ?: descriptor.defaults.callTpl,
                        jsonCallTpl = it.jsonCallTpl ?: descriptor.defaults.jsonCallTpl,
                        simpleClassName = it.simpleClassName ?: descriptor.defaults.simpleClassName,
                    )
                }

                descriptor.copy(constraints = constraints)
            }
    }

    fun constraintImports(): List<String> {
        return constraintDescriptors.flatMap { it.imports }.toSet().sorted()
    }

    fun jsonConstraintImports(): List<String> {
        return constraintDescriptors.flatMap { it.jsonImports }.sorted()
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

        val matchedConstraint = annotations
            .mapNotNull { constraint ->
                val descriptor = constraintDescriptors
                    .flatMap { it.constraints }
                    .find { descriptor ->
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

        return matchedConstraint
    }
}
