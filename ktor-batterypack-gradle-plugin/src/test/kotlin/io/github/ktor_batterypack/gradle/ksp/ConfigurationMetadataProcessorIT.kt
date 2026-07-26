package io.github.ktor_batterypack.gradle.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper

@OptIn(ExperimentalCompilerApi::class)
class ConfigurationMetadataProcessorIT {

    private val mapper = ObjectMapper()

    private fun loadFixture(name: String): tools.jackson.databind.JsonNode {
        val stream = javaClass.classLoader.getResourceAsStream("fixtures/$name")
            ?: throw IllegalArgumentException("Fixture not found: $name")
        return mapper.readTree(stream)
    }

    @Test
    fun `should generate metadata for data class with primitive properties`() {
        val compilation = KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin(
                    "SimpleProps.kt", """
                        package test

                        import com.fasterxml.jackson.annotation.JsonPropertyDescription

                        data class SimpleProps(
                            @param:JsonPropertyDescription("The hostname to bind to")
                            val host: String = "localhost",
                            @param:JsonPropertyDescription("The port to listen on")
                            val port: Int = 8080,
                            val enabled: Boolean = true,
                            val timeoutMs: Long = 5000L,
                            val factor: Double = 1.5,
                            val ratio: Float = 0.75f
                        )
                    """.trimIndent()
                )
            )
            configureKsp {
                symbolProcessorProviders += ConfigurationMetadataProcessorProvider()
                processorOptions["configMetadataClass"] = "test.SimpleProps"
            }
            inheritClassPath = true
        }

        val result = compilation.compile()
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        val metadataFile =
            compilation.workingDir.resolve("ksp/sources/resources/META-INF/spring-configuration-metadata.json")
        assertThat(metadataFile).exists()

        val expected = loadFixture("it_primitive_properties.json")
        val actual = mapper.readTree(metadataFile)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `should generate metadata for data class with nested data classes`() {
        val compilation = KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin(
                    "AppConfig.kt", """
                        package test
                        data class AppConfig(
                            val server: ServerConfig = ServerConfig(),
                            val database: DatabaseConfig = DatabaseConfig()
                        )
                        data class ServerConfig(
                            val host: String = "0.0.0.0",
                            val port: Int = 8080
                        )
                        data class DatabaseConfig(
                            val url: String = "jdbc:postgresql://localhost:5432/db",
                            val poolSize: Int = 10
                        )
                    """.trimIndent()
                )
            )
            configureKsp {
                symbolProcessorProviders += ConfigurationMetadataProcessorProvider()
                processorOptions["configMetadataClass"] = "test.AppConfig"
            }
            inheritClassPath = true
        }

        val result = compilation.compile()
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        val metadataFile =
            compilation.workingDir.resolve("ksp/sources/resources/META-INF/spring-configuration-metadata.json")
        assertThat(metadataFile).exists()

        val expected = loadFixture("it_nested_classes.json")
        val actual = mapper.readTree(metadataFile)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `should log error when class not found`() {
        val compilation = KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin(
                    "Empty.kt", """
                        package test
                        data class Empty(val x: String = "")
                    """.trimIndent()
                )
            )
            configureKsp {
                symbolProcessorProviders += ConfigurationMetadataProcessorProvider()
                processorOptions["configMetadataClass"] = "test.NonExistent"
            }
            inheritClassPath = true
        }

        val result = compilation.compile()
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("ConfigurationMetadataProcessor: class test.NonExistent not found")
    }
}
