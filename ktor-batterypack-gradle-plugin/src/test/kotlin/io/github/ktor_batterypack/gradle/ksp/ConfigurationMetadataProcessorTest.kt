package io.github.ktor_batterypack.gradle.ksp

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tools.jackson.databind.ObjectMapper

class ConfigurationMetadataProcessorTest {

    private val mapper = ObjectMapper()

    private fun loadFixture(name: String): tools.jackson.databind.JsonNode {
        val stream = javaClass.classLoader.getResourceAsStream("fixtures/$name")
            ?: throw IllegalArgumentException("Fixture not found: $name")
        return mapper.readTree(stream)
    }

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

    @Nested
    inner class JsonOutput {

        @Test
        fun `flat properties`() {
            val expected = loadFixture("flat_properties.json")

            val json = buildMetadataJson(
                listOf(
                    ConfigurationMetadataProcessor.PropertyMeta("host", "java.lang.String", "com.example.ServerProps"),
                    ConfigurationMetadataProcessor.PropertyMeta("port", "java.lang.Integer", "com.example.ServerProps"),
                ),
                emptyList()
            )

            assertThat(mapper.readTree(json)).isEqualTo(expected)
        }

        @Test
        fun `empty input`() {
            val expected = loadFixture("empty.json")

            val json = buildMetadataJson(emptyList(), emptyList())

            assertThat(mapper.readTree(json)).isEqualTo(expected)
        }

        @Test
        fun `single-level nesting`() {
            val expected = loadFixture("single_nesting.json")

            val json = buildMetadataJson(
                listOf(
                    ConfigurationMetadataProcessor.PropertyMeta("server.host", "java.lang.String", "com.example.ServerProps"),
                    ConfigurationMetadataProcessor.PropertyMeta("server.port", "java.lang.Integer", "com.example.ServerProps"),
                    ConfigurationMetadataProcessor.PropertyMeta("name", "java.lang.String", "com.example.AppConfig"),
                ),
                listOf(
                    GroupMeta("server", "com.example.ServerProps", "com.example.AppConfig"),
                )
            )

            assertThat(mapper.readTree(json)).isEqualTo(expected)
        }

        @Test
        fun `three-level deep nesting`() {
            val expected = loadFixture("deep_nesting.json")

            val json = buildMetadataJson(
                listOf(
                    ConfigurationMetadataProcessor.PropertyMeta("redis.fetcher.consumerPrefix", "java.lang.String", "com.example.FetcherProps"),
                    ConfigurationMetadataProcessor.PropertyMeta("redis.fetcher.fetchingTimeout", "java.lang.Long", "com.example.FetcherProps"),
                    ConfigurationMetadataProcessor.PropertyMeta("redis.url", "java.lang.String", "com.example.RedisProps"),
                ),
                listOf(
                    GroupMeta("redis.fetcher", "com.example.FetcherProps", "com.example.RedisProps"),
                    GroupMeta("redis", "com.example.RedisProps", "com.example.AppConfig"),
                )
            )

            assertThat(mapper.readTree(json)).isEqualTo(expected)
        }

        @Test
        fun `group without leaf properties`() {
            val expected = loadFixture("group_no_leaf.json")

            val json = buildMetadataJson(
                listOf(
                    ConfigurationMetadataProcessor.PropertyMeta("name", "java.lang.String", "com.example.RootProps"),
                ),
                listOf(
                    GroupMeta("empty", "com.example.EmptyProps", "com.example.RootProps"),
                )
            )

            assertThat(mapper.readTree(json)).isEqualTo(expected)
        }

        @Test
        fun `mixed flat and nested`() {
            val expected = loadFixture("mixed_flat_nested.json")

            val json = buildMetadataJson(
                listOf(
                    ConfigurationMetadataProcessor.PropertyMeta("server.host", "java.lang.String", "com.example.ServerProps"),
                    ConfigurationMetadataProcessor.PropertyMeta("server.port", "java.lang.Integer", "com.example.ServerProps"),
                    ConfigurationMetadataProcessor.PropertyMeta("boo", "java.lang.String", "com.example.AppConfig"),
                    ConfigurationMetadataProcessor.PropertyMeta("foo", "java.lang.String", "com.example.AppConfig"),
                ),
                listOf(
                    GroupMeta("server", "com.example.ServerProps", "com.example.AppConfig"),
                )
            )

            assertThat(mapper.readTree(json)).isEqualTo(expected)
        }

        @Test
        fun `multiple nested siblings`() {
            val expected = loadFixture("multiple_siblings.json")

            val json = buildMetadataJson(
                listOf(
                    ConfigurationMetadataProcessor.PropertyMeta("server.host", "java.lang.String", "com.example.ServerProps"),
                    ConfigurationMetadataProcessor.PropertyMeta("database.url", "java.lang.String", "com.example.DatabaseProps"),
                    ConfigurationMetadataProcessor.PropertyMeta("database.poolSize", "java.lang.Integer", "com.example.DatabaseProps"),
                ),
                listOf(
                    GroupMeta("server", "com.example.ServerProps", "com.example.AppConfig"),
                    GroupMeta("database", "com.example.DatabaseProps", "com.example.AppConfig"),
                )
            )

            assertThat(mapper.readTree(json)).isEqualTo(expected)
        }

        @Test
        fun `no groups section when no nested data classes`() {
            val expected = loadFixture("no_groups.json")

            val json = buildMetadataJson(
                listOf(
                    ConfigurationMetadataProcessor.PropertyMeta("host", "java.lang.String", "com.example.FlatProps"),
                ),
                emptyList()
            )

            assertThat(mapper.readTree(json)).isEqualTo(expected)
        }
    }
}
