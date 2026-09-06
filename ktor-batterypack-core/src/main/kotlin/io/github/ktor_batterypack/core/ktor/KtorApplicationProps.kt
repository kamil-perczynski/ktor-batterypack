package io.github.ktor_batterypack.core.ktor

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

/**
 * Application-specific Ktor settings.
 */
data class KtorApplicationProps(
    @param:JsonPropertyDescription("List of fully-qualified module class names to load")
    @field:NotEmpty
    val modules: List<@NotBlank String> = emptyList()
)
