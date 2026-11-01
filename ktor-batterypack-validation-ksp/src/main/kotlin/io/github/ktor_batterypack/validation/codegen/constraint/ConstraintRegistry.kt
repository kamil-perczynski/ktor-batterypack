package io.github.ktor_batterypack.validation.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import io.github.ktor_batterypack.validation.ksp.CodegenAnnotation
import io.github.ktor_batterypack.validation.ksp.CodegenMode
import tools.jackson.dataformat.yaml.YAMLMapper

class ConstraintRegistry(sources: List<ConstraintDescriptionSource>) {

    companion object {
        fun default(): ConstraintRegistry {
            return ConstraintRegistry(listOf(JakartaConstraintsSource()))
        }
    }

    private val registeredConstraints: List<RegisteredConstraint>

    init {
        val yamlMapper = YAMLMapper.builder()
            .build()

        registeredConstraints = sources
            .map { source -> source.loadConstraintDescriptor(yamlMapper) }
            .flatMap { descriptor ->
                descriptor.constraints.map { constraint -> register(descriptor.defaults, constraint) }
            }
    }

    fun findDescriptor(annotationFqName: String): ConstraintsDescriptor.Constraint? {
        return findRegistered(annotationFqName)?.descriptor
    }

    fun toConstraints(annotations: List<CodegenAnnotation>, mode: CodegenMode): List<ConstraintCall> {
        return unwrapContainerAnnotations(annotations).mapNotNull { annotation ->
            val registered = findRegistered(annotation.fqName) ?: return@mapNotNull null
            ConstraintCall(
                member = if (mode == CodegenMode.JSON) registered.jsonMember else registered.member,
                args = annotation.args,
                rejectsNull = registered.descriptor.rejectsNull == true
            )
        }
    }

    fun unwrapContainerAnnotations(annotations: List<CodegenAnnotation>): List<CodegenAnnotation> {
        return annotations.flatMap { annotation ->
            if (findRegistered(annotation.fqName) != null) {
                return@flatMap listOf(annotation)
            }
            val containedAnnotations = findContainedAnnotations(annotation) ?: return@flatMap listOf(annotation)
            unwrapContainerAnnotations(containedAnnotations)
        }
    }

    private fun findRegistered(annotationFqName: String): RegisteredConstraint? {
        return registeredConstraints.find { registered -> registered.descriptor.annotation == annotationFqName }
    }

    private fun register(
        defaults: ConstraintsDescriptor.Defaults,
        constraint: ConstraintsDescriptor.Constraint
    ): RegisteredConstraint {
        val className = parseClassName(constraint.className ?: defaults.className, constraint)
        val methodName = constraint.methodName ?: "check${constraint.annotation.substringAfterLast('.')}"
        val jsonClassName = constraint.json?.className?.let { name -> parseClassName(name, constraint) } ?: className
        val jsonMethodName = constraint.json?.methodName ?: methodName

        return RegisteredConstraint(
            descriptor = constraint,
            member = MemberName(className, methodName),
            jsonMember = MemberName(jsonClassName, jsonMethodName)
        )
    }

    private fun parseClassName(name: String, constraint: ConstraintsDescriptor.Constraint): ClassName {
        val className = try {
            ClassName.bestGuess(name)
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException(notFullyQualifiedMessage(name, constraint), e)
        }
        check(className.packageName.isNotEmpty()) { notFullyQualifiedMessage(name, constraint) }
        return className
    }

    private fun notFullyQualifiedMessage(name: String, constraint: ConstraintsDescriptor.Constraint): String {
        return "Constraint ${constraint.annotation} refers to `$name`, which is not a fully qualified class name"
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

    private data class RegisteredConstraint(
        val descriptor: ConstraintsDescriptor.Constraint,
        val member: MemberName,
        val jsonMember: MemberName
    )
}

private const val CONTAINER_VALUE_ARG = "value"
