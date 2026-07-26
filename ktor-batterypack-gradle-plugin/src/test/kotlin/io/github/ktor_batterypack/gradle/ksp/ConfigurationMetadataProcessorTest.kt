package io.github.ktor_batterypack.gradle.ksp

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ConfigurationMetadataProcessorTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(
        "kotlin.String, java.lang.String",
        "kotlin.Int, java.lang.Integer",
        "kotlin.Long, java.lang.Long",
        "kotlin.Boolean, java.lang.Boolean",
        "kotlin.Double, java.lang.Double",
        "kotlin.Float, java.lang.Float",
        "kotlin.collections.List, java.util.List",
        "kotlin.collections.MutableList, java.util.List",
        "kotlin.collections.Set, java.util.Set",
        "kotlin.collections.MutableSet, java.util.Set",
        "kotlin.collections.Map, java.util.Map",
        "kotlin.collections.MutableMap, java.util.Map",
        "io.example.FooProps, io.example.FooProps",
    )
    fun `map Kotlin types to Java types`(kotlinType: String, expectedJavaType: String) {
        assertThat(mapKotlinTypeToJava(kotlinType)).isEqualTo(expectedJavaType)
    }

    @Test
    fun `null type maps to Object`() {
        assertThat(mapKotlinTypeToJava(null)).isEqualTo("java.lang.Object")
    }
}
