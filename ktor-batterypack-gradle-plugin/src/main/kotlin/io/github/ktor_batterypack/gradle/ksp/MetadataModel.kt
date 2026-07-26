package io.github.ktor_batterypack.gradle.ksp

data class SpringConfigMetadata(
    val properties: List<ConfigProperty> = emptyList(),
    val groups: List<ConfigGroup> = emptyList()
)

data class ConfigProperty(
    val name: String,
    val type: String,
    val sourceType: String,
    val description: String? = null
)

data class ConfigGroup(
    val name: String,
    val type: String,
    val sourceType: String
)
