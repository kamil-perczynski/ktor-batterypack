package io.github.ktor_batterypack.validation.codegen

import io.github.ktor_batterypack.validation.Constraints
import io.github.ktor_batterypack.validation.ksp.CodegenAnnotation
import io.github.ktor_batterypack.validation.reflection.toReflectionCodegenType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.dataformat.yaml.YAMLMapper
import java.time.temporal.Temporal
import kotlin.reflect.typeOf

class ConstraintsDescriptorTest {

    companion object {
        private val JAKARTA_CONSTRAINTS = listOf(
            "NotNull",
            "Null",
            "AssertTrue",
            "AssertFalse",
            "Digits",
            "NotBlank",
            "NotEmpty",
            "Min",
            "Max",
            "Size",
            "Past",
            "PastOrPresent",
            "Pattern",
            "DecimalMax",
            "DecimalMin",
            "Email",
            "Future",
            "FutureOrPresent",
            "Negative",
            "NegativeOrZero",
            "Positive",
            "PositiveOrZero",
        )
    }

    @Test
    fun `jakarta descriptor declares every supported jakarta constraint`() {
        val descriptor = JakartaConstraintsSource().loadConstraintDescriptor(YAMLMapper.builder().build())

        assertThat(descriptor.imports).containsExactly("io.github.ktor_batterypack.validation.Constraints")
        assertThat(descriptor.jsonImports).containsExactly(
            "io.github.ktor_batterypack.validation.Constraints",
            "io.github.ktor_batterypack.validation.JsonConstraints",
        )
        assertThat(descriptor.defaults.simpleClassName).isEqualTo("Constraints")
        assertThat(descriptor.constraints.map { constraint -> constraint.annotation })
            .containsExactlyInAnyOrderElementsOf(JAKARTA_CONSTRAINTS.map { name -> "jakarta.validation.constraints.$name" })
    }

    @Test
    fun `registry applies descriptor defaults and keeps per constraint overrides`() {
        val annotations = JAKARTA_CONSTRAINTS.map { name ->
            StubAnnotation(name, "jakarta.validation.constraints.$name")
        }

        val constraints = ConstraintRegistry.default().toConstraints(toReflectionCodegenType(typeOf<String>()), annotations)

        assertThat(constraints.keys).containsExactlyInAnyOrderElementsOf(JAKARTA_CONSTRAINTS)
        assertThat(constraints.values.map { constraint -> constraint.descriptor.simpleClassName }).containsOnly("Constraints")
        assertThat(constraints.getValue("NotBlank").name).isEqualTo("checkNotBlank")
        assertThat(constraints.getValue("NotEmpty").descriptor.jsonCallTpl)
            .isEqualTo("JsonConstraints.checkNotEmpty({{quote prop}}, {{value}}, {{call}})")
        assertThat(constraints.getValue("Size").descriptor.jsonCallTpl)
            .isEqualTo("JsonConstraints.checkSize({{quote prop}}, {{value}}, {{call}}, {{constraintArgs args}})")
        assertThat(constraints.filterKeys { name -> name != "NotEmpty" && name != "Size" }.values.map { constraint -> constraint.descriptor.jsonCallTpl })
            .containsOnlyNulls()
    }

    @Test
    fun `registry ignores annotations without a descriptor`() {
        val annotations = listOf(StubAnnotation("Deprecated", "kotlin.Deprecated"))

        val constraints = ConstraintRegistry.default().toConstraints(toReflectionCodegenType(typeOf<String>()), annotations)

        assertThat(constraints).isEmpty()
    }

    @Test
    fun `every supported type has a runtime check overload`() {
        val descriptor = JakartaConstraintsSource().loadConstraintDescriptor(YAMLMapper.builder().build())
        val methods = Constraints::class.java.methods

        val missing = descriptor.constraints.flatMap { constraint ->
            val methodName = "check" + constraint.annotation.substringAfterLast('.')
            constraint.supportedTypes.orEmpty()
                .flatMap { supportedType -> toJvmClasses(supportedType) }
                .filter { jvmClass ->
                    methods.none { method -> method.name == methodName && method.parameterTypes[1] == jvmClass }
                }
                .map { jvmClass -> "$methodName(${jvmClass.name})" }
        }

        assertThat(missing).isEmpty()
    }

    @Test
    fun `jakarta constraints except null checks declare supported types`() {
        val descriptor = JakartaConstraintsSource().loadConstraintDescriptor(YAMLMapper.builder().build())

        assertThat(descriptor.constraints.filter { constraint -> constraint.supportedTypes == null }.map { it.annotation })
            .containsExactlyInAnyOrder("jakarta.validation.constraints.NotNull", "jakarta.validation.constraints.Null")
    }

    private fun toJvmClasses(supportedType: String): List<Class<*>> {
        return when (supportedType) {
            "kotlin.Byte" -> listOf(Byte::class.javaPrimitiveType!!)
            "kotlin.Short" -> listOf(Short::class.javaPrimitiveType!!)
            "kotlin.Int" -> listOf(Int::class.javaPrimitiveType!!)
            "kotlin.Long" -> listOf(Long::class.javaPrimitiveType!!)
            "kotlin.Float" -> listOf(Float::class.javaPrimitiveType!!)
            "kotlin.Double" -> listOf(Double::class.javaPrimitiveType!!)
            "kotlin.Boolean" -> listOf(Boolean::class.javaPrimitiveType!!)
            "kotlin.String" -> listOf(String::class.java)
            "kotlin.collections.Collection" -> listOf(Collection::class.java, Array<Any>::class.java)
            "kotlin.collections.Map" -> listOf(Map::class.java)
            "java.time.LocalDate", "java.time.LocalDateTime", "java.time.OffsetDateTime",
            "java.time.ZonedDateTime", "java.time.Instant" -> listOf(Temporal::class.java)
            else -> listOf(Class.forName(supportedType))
        }
    }

    private data class StubAnnotation(
        override val name: String,
        override val fqName: String,
        override val args: Map<String, Any> = emptyMap(),
        override val metaAnnotations: List<String> = emptyList(),
    ) : CodegenAnnotation

}
