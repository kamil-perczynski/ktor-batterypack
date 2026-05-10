package io.github.kperczynski.libs.ktor.multipart

data class MultipartProps(
    val maxFileSizeBytes: Int = 5 * 1024 * 1024,
    val allowedContentTypes: List<String> = listOf("image/jpeg", "image/png", "image/webp")
)
