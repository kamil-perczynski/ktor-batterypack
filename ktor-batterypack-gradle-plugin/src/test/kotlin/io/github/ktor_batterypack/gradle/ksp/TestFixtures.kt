package io.github.ktor_batterypack.gradle.ksp

import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.io.InputStream

object TestFixtures {

    private val jsonMapper = JsonMapper.builder().build()

    fun loadJsonFixture(path: String): JsonNode {
        val stream = fixtureStream(path) ?: throw IllegalArgumentException("Fixture not found: $path")
        return jsonMapper.readTree(stream)
    }

    fun loadFixtureText(path: String): String =
        fixtureStream(path)?.bufferedReader()?.readText()
            ?: throw IllegalArgumentException("Fixture not found: $path")

    private fun fixtureStream(path: String): InputStream? =
        TestFixtures::class.java.classLoader.getResourceAsStream(path)
}
