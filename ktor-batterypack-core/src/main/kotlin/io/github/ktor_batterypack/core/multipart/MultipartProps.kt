package io.github.ktor_batterypack.core.multipart

/**
 * Configuration properties for multipart file uploads.
 *
 * @property maxFileSizeBytes Maximum allowed file size in bytes (default: 5 MiB).
 * @property allowedContentTypes List of allowed MIME types for uploaded files.
 */
data class MultipartProps(
    val maxFileSizeBytes: Int = 5 * 1024 * 1024,
    val allowedContentTypes: List<String> = listOf("image/jpeg", "image/png", "image/webp")
)
