package io.github.ktor_batterypack.core.multipart

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * Configuration properties for multipart file uploads.
 */
data class MultipartProps(
    @param:JsonPropertyDescription("Maximum allowed file size in bytes")
    val maxFileSizeBytes: Int = 5 * 1024 * 1024,
    @param:JsonPropertyDescription("List of allowed MIME types for uploaded files")
    @field:NotEmpty
    val allowedContentTypes: List<@NotBlank String> = listOf(
        "image/jpeg",
        "image/png",
        "image/webp"
    )
)
