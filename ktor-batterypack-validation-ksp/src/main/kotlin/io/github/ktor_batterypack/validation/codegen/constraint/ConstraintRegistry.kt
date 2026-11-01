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


    fun findDescriptor(annotationFqName: String): ConstraintsDescriptor.Constraint? {
        return constraintDescriptors
            .asSequence()
            .flatMap { descriptor -> descriptor.constraints }
            .find { constraint -> constraint.annotation == annotationFqName }
    }

    fun findItemConstraints(codegenType: CodegenType): List<ConstraintMethod> {
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
    ): List<ConstraintMethod> {
        return unwrapContainerAnnotations(annotations).mapNotNull { annotation ->
            val descriptor = findDescriptor(annotation.fqName) ?: return@mapNotNull null
            ConstraintMethod(
                name = descriptor.methodName ?: "check${annotation.name}",
                args = annotation.args,
                descriptor = descriptor,
            )
        }
    }

    fun unwrapContainerAnnotations(annotations: List<CodegenAnnotation>): List<CodegenAnnotation> {
        return annotations.flatMap { annotation ->
            if (findDescriptor(annotation.fqName) != null) {
                return@flatMap listOf(annotation)
            }
            val containedAnnotations = findContainedAnnotations(annotation) ?: return@flatMap listOf(annotation)
            unwrapContainerAnnotations(containedAnnotations)
        }
    }

    private fun findContainedAnnotations(annotation: CodegenAnnotation): List<CodegenAnnotation>? {
        if (annotation.args.keys != setOf(CONTAINER_VALUE_ARG)) {
            return null
        }
        val value = annotation.args.getValue(CONTAINER_VALUE_ARG) as? List<*> ?: return null
        if (value.any { item -> item !is CodegenAnnotation }) {
            return null
        }
        return value.filterIsInstance<CodegenAnnotation>()
    }
}

private const val CONTAINER_VALUE_ARG = "value"
