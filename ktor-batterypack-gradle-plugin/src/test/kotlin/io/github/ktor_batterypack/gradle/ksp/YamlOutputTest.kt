package io.github.ktor_batterypack.gradle.ksp

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.dataformat.yaml.YAMLMapper

class YamlOutputTest {

    private val yamlMapper = YAMLMapper.builder().build()

    @Test
    fun `flat properties`() {
        val expected = TestFixtures.loadFixtureText("fixtures/flat_properties.yaml")
        val yaml = MetadataWriter.writeYaml(
            SpringConfigMetadata(
                properties = listOf(
                    ConfigProperty("host", "java.lang.String", "com.example.ServerProps"),
                    ConfigProperty("port", "java.lang.Integer", "com.example.ServerProps"),
                )
            )
        )
        assertThat(yamlMapper.readTree(yaml))
            .isEqualTo(yamlMapper.readTree(expected))
    }

    @Test
    fun `empty input`() {
        val expected = TestFixtures.loadFixtureText("fixtures/empty.yaml")
        val yaml = MetadataWriter.writeYaml(SpringConfigMetadata())
        assertThat(yamlMapper.readTree(yaml))
            .isEqualTo(yamlMapper.readTree(expected))
    }

    @Test
    fun `single-level nesting`() {
        val expected = TestFixtures.loadFixtureText("fixtures/single_nesting.yaml")
        val yaml = MetadataWriter.writeYaml(
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
        assertThat(yamlMapper.readTree(yaml))
            .isEqualTo(yamlMapper.readTree(expected))
    }

    @Test
    fun `three-level deep nesting`() {
        val expected = TestFixtures.loadFixtureText("fixtures/deep_nesting.yaml")
        val yaml = MetadataWriter.writeYaml(
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
        assertThat(yamlMapper.readTree(yaml))
            .isEqualTo(yamlMapper.readTree(expected))
    }

    @Test
    fun `group without leaf properties`() {
        val expected = TestFixtures.loadFixtureText("fixtures/group_no_leaf.yaml")
        val yaml = MetadataWriter.writeYaml(
            SpringConfigMetadata(
                properties = listOf(
                    ConfigProperty("name", "java.lang.String", "com.example.RootProps"),
                ),
                groups = listOf(
                    ConfigGroup("empty", "com.example.EmptyProps", "com.example.RootProps"),
                )
            )
        )
        assertThat(yamlMapper.readTree(yaml))
            .isEqualTo(yamlMapper.readTree(expected))
    }

    @Test
    fun `mixed flat and nested`() {
        val expected = TestFixtures.loadFixtureText("fixtures/mixed_flat_nested.yaml")
        val yaml = MetadataWriter.writeYaml(
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
        assertThat(yamlMapper.readTree(yaml))
            .isEqualTo(yamlMapper.readTree(expected))
    }

    @Test
    fun `multiple nested siblings`() {
        val expected = TestFixtures.loadFixtureText("fixtures/multiple_siblings.yaml")
        val yaml = MetadataWriter.writeYaml(
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
        assertThat(yamlMapper.readTree(yaml))
            .isEqualTo(yamlMapper.readTree(expected))
    }

    @Test
    fun `no groups section when no nested data classes`() {
        val expected = TestFixtures.loadFixtureText("fixtures/no_groups.yaml")
        val yaml = MetadataWriter.writeYaml(
            SpringConfigMetadata(
                properties = listOf(
                    ConfigProperty("host", "java.lang.String", "com.example.FlatProps"),
                )
            )
        )
        assertThat(yamlMapper.readTree(yaml))
            .isEqualTo(yamlMapper.readTree(expected))
    }
}
