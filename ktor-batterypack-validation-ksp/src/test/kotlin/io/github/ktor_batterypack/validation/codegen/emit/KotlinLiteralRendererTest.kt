package io.github.ktor_batterypack.validation.codegen.emit

import com.squareup.kotlinpoet.ClassName
import io.github.ktor_batterypack.validation.ksp.AnnotationValue
import io.github.ktor_batterypack.validation.ksp.CodegenAnnotation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KotlinLiteralRendererTest {

    @Test
    fun `strings escape backslashes, quotes, dollars and control characters`() {
        assertThat(render("a\\b\"c\$d\${e}\nf\rg\th"))
            .isEqualTo("\"a\\\\b\\\"c\\\$d\\\${e}\\nf\\rg\\th\"")
    }

    @Test
    fun `chars render as char literals`() {
        assertThat(render('x')).isEqualTo("'x'")
        assertThat(render('\'')).isEqualTo("'\\''")
        assertThat(render('$')).isEqualTo("'$'")
    }

    @Test
    fun `dollars that cannot start a template stay unescaped`() {
        assertThat(render("$")).isEqualTo("\"$\"")
        assertThat(render("^\\d+$")).isEqualTo("\"^\\\\d+$\"")
        assertThat(render("\$_x \$`y`")).isEqualTo("\"\\\$_x \\\$`y`\"")
    }

    @Test
    fun `numbers render as kotlin literals`() {
        assertThat(render(3_000_000_000L)).isEqualTo("3000000000")
        assertThat(render(-5L)).isEqualTo("-5")
        assertThat(render(Long.MIN_VALUE)).isEqualTo("Long.MIN_VALUE")
        assertThat(render(1.5f)).isEqualTo("1.5f")
        assertThat(render(Float.NaN)).isEqualTo("Float.NaN")
        assertThat(render(Float.NEGATIVE_INFINITY)).isEqualTo("Float.NEGATIVE_INFINITY")
        assertThat(render(2.5)).isEqualTo("2.5")
        assertThat(render(Double.POSITIVE_INFINITY)).isEqualTo("Double.POSITIVE_INFINITY")
        assertThat(render(7)).isEqualTo("7")
        assertThat(render(true)).isEqualTo("true")
    }

    @Test
    fun `enum entries, classes, lists and nested annotations render as expressions`() {
        val nested = StubAnnotation(
            "com.example.Range",
            mapOf("from" to 1, "unit" to AnnotationValue.EnumEntry(ClassName("java.util.concurrent", "TimeUnit"), "SECONDS"))
        )

        assertThat(render(AnnotationValue.EnumEntry(ClassName("com.example", "Mode"), "STRICT")))
            .isEqualTo("com.example.Mode.STRICT")
        assertThat(render(AnnotationValue.ClassReference(ClassName("kotlin", "String"))))
            .isEqualTo("kotlin.String::class")
        assertThat(render(listOf("a", 1)))
            .isEqualTo("arrayOf(\"a\", 1)")
        assertThat(render(nested))
            .isEqualTo("com.example.Range(from = 1, unit = java.util.concurrent.TimeUnit.SECONDS)")
    }

    @Test
    fun `strings escape kotlinpoet placeholder characters`() {
        assertThat(render("a·b♢c")).isEqualTo("\"a\\u00B7b\\u2662c\"")
    }

    @Test
    fun `args render as named arguments`() {
        assertThat(KotlinLiteralRenderer.renderArgs(mapOf("min" to 1L, "message" to "too short")).toString())
            .isEqualTo("min = 1, message = \"too short\"")
    }

    private fun render(value: Any?): String {
        return KotlinLiteralRenderer.render(value).toString()
    }

    private data class StubAnnotation(
        override val fqName: String,
        override val args: Map<String, Any>,
    ) : CodegenAnnotation {
        override val name: String
            get() = fqName.substringAfterLast('.')
        override val className: ClassName
            get() = ClassName.bestGuess(fqName)
        override val metaAnnotations: List<String>
            get() = emptyList()
    }

}
