package io.github.ktor_batterypack.validation.codegen

import com.squareup.kotlinpoet.ClassName
import io.github.ktor_batterypack.validation.Constraints
import io.github.ktor_batterypack.validation.ksp.CodegenAnnotation
import io.github.ktor_batterypack.validation.ksp.CodegenMode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import tools.jackson.dataformat.yaml.YAMLMapper
import java.time.temporal.Temporal

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

        assertThat(descriptor.defaults.className).isEqualTo("io.github.ktor_batterypack.validation.Constraints")
        assertThat(descriptor.constraints.map { constraint -> constraint.annotation })
            .containsExactlyInAnyOrderElementsOf(JAKARTA_CONSTRAINTS.map { name -> "jakarta.validation.constraints.$name" })
    }

    @Test
    fun `registry applies descriptor defaults and keeps per constraint overrides`() {
        val annotations = JAKARTA_CONSTRAINTS.map { name ->
            StubAnnotation(name, "jakarta.validation.constraints.$name")
        }

        val typed = ConstraintRegistry.default().toConstraints(annotations, CodegenMode.TYPED)
        val json = ConstraintRegistry.default().toConstraints(annotations, CodegenMode.JSON)

        assertThat(typed.map { constraint -> constraint.member.toString() })
            .containsExactlyElementsOf(JAKARTA_CONSTRAINTS.map { name -> "io.github.ktor_batterypack.validation.Constraints.check$name" })
        assertThat(json.map { constraint -> constraint.member.toString() })
            .containsExactlyElementsOf(
                JAKARTA_CONSTRAINTS.map { name ->
                    val owner = if (name == "NotEmpty" || name == "Size") "JsonConstraints" else "Constraints"
                    "io.github.ktor_batterypack.validation.$owner.check$name"
                }
            )
        assertThat(typed.filter { constraint -> constraint.rejectsNull }.map { constraint -> constraint.member.simpleName })
            .containsExactlyInAnyOrder("checkNotNull", "checkNotBlank", "checkNotEmpty")
    }

    @Test
    fun `registry keeps repeated constraints in declaration order`() {
        val annotations = listOf(
            StubAnnotation("Pattern", "jakarta.validation.constraints.Pattern", mapOf("regexp" to "^a")),
            StubAnnotation("NotBlank", "jakarta.validation.constraints.NotBlank"),
            StubAnnotation("Pattern", "jakarta.validation.constraints.Pattern", mapOf("regexp" to "z$")),
        )

        val constraints = ConstraintRegistry.default().toConstraints(annotations, CodegenMode.TYPED)

        assertThat(constraints.map { constraint -> constraint.member.simpleName to constraint.args })
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

        val constraints = ConstraintRegistry.default().toConstraints(listOf(container), CodegenMode.TYPED)

        assertThat(constraints.map { constraint -> constraint.args })
            .containsExactly(mapOf("regexp" to "^a"), mapOf("regexp" to "z$"))
    }

    @Test
    fun `registry keeps constraints sharing a simple name apart`() {
        val customDescriptor = """
            defaults:
              className: com.example.EmailRules
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

        val constraints = registry.toConstraints(annotations, CodegenMode.TYPED)

        assertThat(constraints.map { constraint -> constraint.member.toString() })
            .containsExactly("io.github.ktor_batterypack.validation.Constraints.checkEmail", "com.example.EmailRules.checkCorporateEmail")
    }

    @Test
    fun `registry applies json overrides of class and method`() {
        val customDescriptor = """
            defaults:
              className: com.example.Rules
            constraints:
              - annotation: com.example.Iban
                json:
                  className: com.example.JsonRules
                  methodName: checkIbanNode
              - annotation: com.example.Vat
                json:
                  methodName: checkVatNode
        """.trimIndent()
        val registry = ConstraintRegistry(
            listOf(
                ConstraintDescriptionSource { yamlMapper ->
                    yamlMapper.readValue(customDescriptor, ConstraintsDescriptor::class.java)
                }
            )
        )
        val annotations = listOf(
            StubAnnotation("Iban", "com.example.Iban"),
            StubAnnotation("Vat", "com.example.Vat"),
        )

        assertThat(registry.toConstraints(annotations, CodegenMode.TYPED).map { constraint -> constraint.member.toString() })
            .containsExactly("com.example.Rules.checkIban", "com.example.Rules.checkVat")
        assertThat(registry.toConstraints(annotations, CodegenMode.JSON).map { constraint -> constraint.member.toString() })
            .containsExactly("com.example.JsonRules.checkIbanNode", "com.example.Rules.checkVatNode")
    }

    @Test
    fun `registry rejects class names that are not fully qualified`() {
        val customDescriptor = """
            defaults:
              className: Rules
            constraints:
              - annotation: com.example.Iban
        """.trimIndent()

        assertThatThrownBy {
            ConstraintRegistry(
                listOf(
                    ConstraintDescriptionSource { yamlMapper ->
                        yamlMapper.readValue(customDescriptor, ConstraintsDescriptor::class.java)
                    }
                )
            )
        }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Constraint com.example.Iban refers to `Rules`, which is not a fully qualified class name")
    }

    @Test
    fun `registry ignores annotations without a descriptor`() {
        val annotations = listOf(StubAnnotation("Deprecated", "kotlin.Deprecated"))

        val constraints = ConstraintRegistry.default().toConstraints(annotations, CodegenMode.TYPED)

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
    ) : CodegenAnnotation {
        override val className: ClassName
            get() = ClassName.bestGuess(fqName)
    }

}
