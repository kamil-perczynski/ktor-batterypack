package io.github.ktor_batterypack.validation.codegen

import io.github.ktor_batterypack.validation.codegen.helper.KotlinLiteralRenderer
import io.github.ktor_batterypack.validation.ksp.AnnotationValue
import io.github.ktor_batterypack.validation.ksp.CodegenAnnotation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KotlinLiteralRendererTest {

    @Test
    fun `strings escape backslashes, quotes, dollars and control characters`() {
        assertThat(KotlinLiteralRenderer.render("a\\b\"c\$d\${e}\nf\rg\th"))
            .isEqualTo("\"a\\\\b\\\"c\\\$d\\\${e}\\nf\\rg\\th\"")
    }

    @Test
    fun `chars render as char literals`() {
        assertThat(KotlinLiteralRenderer.render('x')).isEqualTo("'x'")
        assertThat(KotlinLiteralRenderer.render('\'')).isEqualTo("'\\''")
        assertThat(KotlinLiteralRenderer.render('$')).isEqualTo("'$'")
    }

    @Test
    fun `dollars that cannot start a template stay unescaped`() {
        assertThat(KotlinLiteralRenderer.render("$")).isEqualTo("\"$\"")
        assertThat(KotlinLiteralRenderer.render("^\\d+$")).isEqualTo("\"^\\\\d+$\"")
        assertThat(KotlinLiteralRenderer.render("\$_x \$`y`")).isEqualTo("\"\\\$_x \\\$`y`\"")
    }

    @Test
    fun `numbers render as kotlin literals`() {
        assertThat(KotlinLiteralRenderer.render(3_000_000_000L)).isEqualTo("3000000000")
        assertThat(KotlinLiteralRenderer.render(-5L)).isEqualTo("-5")
        assertThat(KotlinLiteralRenderer.render(Long.MIN_VALUE)).isEqualTo("Long.MIN_VALUE")
        assertThat(KotlinLiteralRenderer.render(1.5f)).isEqualTo("1.5f")
        assertThat(KotlinLiteralRenderer.render(Float.NaN)).isEqualTo("Float.NaN")
        assertThat(KotlinLiteralRenderer.render(Float.NEGATIVE_INFINITY)).isEqualTo("Float.NEGATIVE_INFINITY")
        assertThat(KotlinLiteralRenderer.render(2.5)).isEqualTo("2.5")
        assertThat(KotlinLiteralRenderer.render(Double.POSITIVE_INFINITY)).isEqualTo("Double.POSITIVE_INFINITY")
        assertThat(KotlinLiteralRenderer.render(7)).isEqualTo("7")
        assertThat(KotlinLiteralRenderer.render(true)).isEqualTo("true")
    }

    @Test
    fun `enum entries, classes, lists and nested annotations render as expressions`() {
        val nested = StubAnnotation(
            "com.example.Range",
            mapOf("from" to 1, "unit" to AnnotationValue.EnumEntry("java.util.concurrent.TimeUnit.SECONDS"))
        )

        assertThat(KotlinLiteralRenderer.render(AnnotationValue.EnumEntry("com.example.Mode.STRICT")))
            .isEqualTo("com.example.Mode.STRICT")
        assertThat(KotlinLiteralRenderer.render(AnnotationValue.ClassReference("kotlin.String")))
            .isEqualTo("kotlin.String::class")
        assertThat(KotlinLiteralRenderer.render(listOf("a", 1)))
            .isEqualTo("arrayOf(\"a\", 1)")
        assertThat(KotlinLiteralRenderer.render(nested))
            .isEqualTo("com.example.Range(from = 1, unit = java.util.concurrent.TimeUnit.SECONDS)")
    }

    private data class StubAnnotation(
        override val fqName: String,
        override val args: Map<String, Any>,
    ) : CodegenAnnotation {
        override val name: String
            get() = fqName.substringAfterLast('.')
        override val metaAnnotations: List<String>
            get() = emptyList()
    }

}
