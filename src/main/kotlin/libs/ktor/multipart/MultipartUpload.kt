package io.github.kperczynski.libs.ktor.multipart

import io.ktor.http.*

/**
 * Represents an uploaded file extracted from multipart form data.
 * This is a domain-friendly representation decoupled from Ktor's PartData.
 */
data class MultipartUpload(
    val filename: String,
    val contentType: ContentType?,
    val bytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MultipartUpload

        if (filename != other.filename) return false
        if (contentType != other.contentType) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = filename.hashCode()
        result = 31 * result + (contentType?.hashCode() ?: 0)
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}
