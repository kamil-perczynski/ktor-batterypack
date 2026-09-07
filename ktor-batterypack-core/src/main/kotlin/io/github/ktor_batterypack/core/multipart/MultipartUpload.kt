package io.github.ktor_batterypack.core.multipart

import io.ktor.http.ContentType

/**
 * Represents a single file extracted from a multipart request.
 *
 * @property filename The original name of the uploaded file.
 * @property contentType The MIME type of the file, or null if unknown.
 * @property bytes The raw file contents as a byte array.
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
