package io.github.ktor_batterypack.core.ktor

/**
 * Application-specific Ktor settings.
 *
 * @property modules List of fully-qualified module class names to load.
 */
data class KtorApplicationProps(
    val modules: List<String> = emptyList()
)
