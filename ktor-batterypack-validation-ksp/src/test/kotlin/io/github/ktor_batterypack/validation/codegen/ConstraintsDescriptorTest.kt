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

        val constraints = ConstraintRegistry.default()
            .toConstraints(toReflectionCodegenType(typeOf<String>()), annotations)
            .associateBy { constraint -> constraint.name.removePrefix("check") }

        assertThat(constraints.keys).containsExactlyElementsOf(JAKARTA_CONSTRAINTS)
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
    fun `registry keeps repeated constraints in declaration order`() {
        val annotations = listOf(
            StubAnnotation("Pattern", "jakarta.validation.constraints.Pattern", mapOf("regexp" to "^a")),
            StubAnnotation("NotBlank", "jakarta.validation.constraints.NotBlank"),
            StubAnnotation("Pattern", "jakarta.validation.constraints.Pattern", mapOf("regexp" to "z$")),
        )

        val constraints = ConstraintRegistry.default().toConstraints(toReflectionCodegenType(typeOf<String>()), annotations)

        assertThat(constraints.map { constraint -> constraint.name to constraint.args })
            .containsExactly(
                "checkPattern" to mapOf("regexp" to "^a"),
                "checkNotBlank" to emptyMap(),
                "checkPattern" to mapOf("regexp" to "z$"),
            )
    }

    @Test
    fun `registry unwraps repeatable constraint containers`() {
        val container = StubAnnotation(
            "List",
            "jakarta.validation.constraints.Pattern.List",
            mapOf(
                "value" to listOf(
                    StubAnnotation("Pattern", "jakarta.validation.constraints.Pattern", mapOf("regexp" to "^a")),
                    StubAnnotation("Pattern", "jakarta.validation.constraints.Pattern", mapOf("regexp" to "z$")),
                )
            )
        )

        val constraints = ConstraintRegistry.default().toConstraints(toReflectionCodegenType(typeOf<String>()), listOf(container))

        assertThat(constraints.map { constraint -> constraint.args })
            .containsExactly(mapOf("regexp" to "^a"), mapOf("regexp" to "z$"))
    }

    @Test
    fun `registry keeps constraints sharing a simple name apart`() {
        val customDescriptor = """
            imports: [com.example.EmailRules]
            jsonImports: [com.example.EmailRules]
            defaults:
              simpleClassName: EmailRules
            constraints:
              - annotation: com.example.Email
                methodName: checkCorporateEmail
        """.trimIndent()
        val registry = ConstraintRegistry(
            listOf(
                JakartaConstraintsSource(),
                ConstraintDescriptionSource { yamlMapper ->
                    yamlMapper.readValue(customDescriptor, ConstraintsDescriptor::class.java)
                }
            )
        )
        val annotations = listOf(
            StubAnnotation("Email", "jakarta.validation.constraints.Email"),
            StubAnnotation("Email", "com.example.Email"),
        )

        val constraints = registry.toConstraints(toReflectionCodegenType(typeOf<String>()), annotations)

        assertThat(constraints.map { constraint -> "${constraint.descriptor.simpleClassName}.${constraint.name}" })
            .containsExactly("Constraints.checkEmail", "EmailRules.checkCorporateEmail")
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
            else -> listOf(toReferenceClass(supportedType))
        }
    }

    private fun toReferenceClass(supportedType: String): Class<*> {
        val jvmClass = Class.forName(supportedType)
        return when {
            Temporal::class.java.isAssignableFrom(jvmClass) -> Temporal::class.java
            else -> jvmClass
        }
    }

    private data class StubAnnotation(
        override val name: String,
        override val fqName: String,
        override val args: Map<String, Any> = emptyMap(),
        override val metaAnnotations: List<String> = emptyList(),
    ) : CodegenAnnotation

}
