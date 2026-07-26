package io.github.ktor_batterypack.gradle.ksp

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class JsonOutputTest {

    private val mapper = JsonMapper.builder().build()

    @Test
    fun `flat properties`() {
        val expected = TestFixtures.loadJsonFixture("fixtures/flat_properties.json")

        val json = MetadataWriter.writeJson(
            SpringConfigMetadata(
                properties = listOf(
                    ConfigProperty("host", "java.lang.String", "com.example.ServerProps"),
                    ConfigProperty("port", "java.lang.Integer", "com.example.ServerProps"),
                )
            )
        )

        assertThat(mapper.readTree(json)).isEqualTo(expected)
    }

    @Test
    fun `empty input`() {
        val expected = TestFixtures.loadJsonFixture("fixtures/empty.json")

        val json = MetadataWriter.writeJson(SpringConfigMetadata())

        assertThat(mapper.readTree(json)).isEqualTo(expected)
    }

    @Test
    fun `single-level nesting`() {
        val expected = TestFixtures.loadJsonFixture("fixtures/single_nesting.json")

        val json = MetadataWriter.writeJson(
            SpringConfigMetadata(
                properties = listOf(
                    ConfigProperty("server.host", "java.lang.String", "com.example.ServerProps"),
                    ConfigProperty("server.port", "java.lang.Integer", "com.example.ServerProps"),
                    ConfigProperty("name", "java.lang.String", "com.example.AppConfig"),
                ),
                groups = listOf(
                    ConfigGroup("server", "com.example.ServerProps", "com.example.AppConfig"),
                )
            )
        )

        assertThat(mapper.readTree(json)).isEqualTo(expected)
    }

    @Test
    fun `three-level deep nesting`() {
        val expected = TestFixtures.loadJsonFixture("fixtures/deep_nesting.json")

        val json = MetadataWriter.writeJson(
            SpringConfigMetadata(
                properties = listOf(
                    ConfigProperty(
                        "redis.fetcher.consumerPrefix",
                        "java.lang.String",
                        "com.example.FetcherProps"
                    ),
                    ConfigProperty(
                        "redis.fetcher.fetchingTimeout",
                        "java.lang.Long",
                        "com.example.FetcherProps"
                    ),
                    ConfigProperty("redis.url", "java.lang.String", "com.example.RedisProps"),
                ),
                groups = listOf(
                    ConfigGroup(
                        "redis.fetcher",
                        "com.example.FetcherProps",
                        "com.example.RedisProps"
                    ),
                    ConfigGroup("redis", "com.example.RedisProps", "com.example.AppConfig"),
                )
            )
        )

        assertThat(mapper.readTree(json)).isEqualTo(expected)
    }

    @Test
    fun `group without leaf properties`() {
        val expected = TestFixtures.loadJsonFixture("fixtures/group_no_leaf.json")

        val json = MetadataWriter.writeJson(
            SpringConfigMetadata(
                properties = listOf(
                    ConfigProperty("name", "java.lang.String", "com.example.RootProps"),
                ),
                groups = listOf(
                    ConfigGroup("empty", "com.example.EmptyProps", "com.example.RootProps"),
                )
            )
        )

        assertThat(mapper.readTree(json)).isEqualTo(expected)
    }

    @Test
    fun `mixed flat and nested`() {
        val expected = TestFixtures.loadJsonFixture("fixtures/mixed_flat_nested.json")

        val json = MetadataWriter.writeJson(
            SpringConfigMetadata(
                properties = listOf(
                    ConfigProperty("server.host", "java.lang.String", "com.example.ServerProps"),
                    ConfigProperty("server.port", "java.lang.Integer", "com.example.ServerProps"),
                    ConfigProperty("boo", "java.lang.String", "com.example.AppConfig"),
                    ConfigProperty("foo", "java.lang.String", "com.example.AppConfig"),
                ),
                groups = listOf(
                    ConfigGroup("server", "com.example.ServerProps", "com.example.AppConfig"),
                )
            )
        )

        assertThat(mapper.readTree(json)).isEqualTo(expected)
    }

    @Test
    fun `multiple nested siblings`() {
        val expected = TestFixtures.loadJsonFixture("fixtures/multiple_siblings.json")

        val json = MetadataWriter.writeJson(
            SpringConfigMetadata(
                properties = listOf(
                    ConfigProperty("server.host", "java.lang.String", "com.example.ServerProps"),
                    ConfigProperty("database.url", "java.lang.String", "com.example.DatabaseProps"),
                    ConfigProperty(
                        "database.poolSize",
                        "java.lang.Integer",
                        "com.example.DatabaseProps"
                    ),
                ),
                groups = listOf(
                    ConfigGroup("server", "com.example.ServerProps", "com.example.AppConfig"),
                    ConfigGroup("database", "com.example.DatabaseProps", "com.example.AppConfig"),
                )
            )
        )

        assertThat(mapper.readTree(json)).isEqualTo(expected)
    }

    @Test
    fun `no groups section when no nested data classes`() {
        val expected = TestFixtures.loadJsonFixture("fixtures/no_groups.json")

        val json = MetadataWriter.writeJson(
            SpringConfigMetadata(
                properties = listOf(
                    ConfigProperty("host", "java.lang.String", "com.example.FlatProps"),
                )
            )
        )

        assertThat(mapper.readTree(json)).isEqualTo(expected)
    }
}
