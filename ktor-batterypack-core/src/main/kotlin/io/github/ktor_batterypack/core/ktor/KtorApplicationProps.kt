package io.github.ktor_batterypack.core.ktor

import com.fasterxml.jackson.annotation.JsonPropertyDescription

/**
 * Application-specific Ktor settings.
 */
data class KtorApplicationProps(
    @param:JsonPropertyDescription("List of fully-qualified module class names to load")
    val modules: List<String> = emptyList()
)
