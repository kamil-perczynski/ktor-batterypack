package io.github.ktor_batterypack.core.multipart

import com.fasterxml.jackson.annotation.JsonPropertyDescription

/**
 * Configuration properties for multipart file uploads.
 */
data class MultipartProps(
    @param:JsonPropertyDescription("Maximum allowed file size in bytes")
    val maxFileSizeBytes: Int = 5 * 1024 * 1024,
    @param:JsonPropertyDescription("List of allowed MIME types for uploaded files")
    val allowedContentTypes: List<String> = listOf("image/jpeg", "image/png", "image/webp")
)
