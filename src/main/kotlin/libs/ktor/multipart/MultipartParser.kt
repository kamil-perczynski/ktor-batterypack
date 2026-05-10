package io.github.kperczynski.libs.ktor.multipart

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.*
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(MultipartParser::class.java)

class MultipartParser(private val props: MultipartProps) {

    private val allowedContentTypes = props.allowedContentTypes
        .map { ContentType.parse(it) }
        .toSet()

    suspend fun parseUploads(multipart: MultiPartData, maxParts: Int): List<MultipartUpload> {
        val uploads = mutableListOf<MultipartUpload>()

        multipart.forEachPart { part ->
            try {
                if (uploads.size >= maxParts) {
                    throw IllegalArgumentException("Too many files uploaded. Maximum allowed is $maxParts.")
                }

                when (part) {
                    is PartData.FileItem -> {
                        val multipartUpload = checkPart(part)
                        uploads.add(multipartUpload)
                    }

                    else -> log.warn("Received unsupported part type: ${part::class.simpleName}")

                }
            } finally {
                part.dispose()
            }
        }

        return uploads
    }

    private suspend fun checkPart(part: PartData.FileItem): MultipartUpload {
        val contentType = part.contentType
        if (!isAllowedContentType(contentType, allowedContentTypes)) {
            throw IllegalArgumentException("Unsupported content type: $contentType")
        }

        val bytes = part.provider().toByteArray()
        if (bytes.size > props.maxFileSizeBytes.toLong()) {
            throw IllegalArgumentException("File size exceeds the maximum allowed size of ${props.maxFileSizeBytes}")
        }

        return MultipartUpload(
            filename = part.originalFileName ?: "upload",
            contentType = part.contentType,
            bytes = bytes
        )
    }

}

private fun isAllowedContentType(
    contentType: ContentType?,
    allowedContenttypes: Set<ContentType>
): Boolean {
    if (contentType == null) {
        return true
    }

    return allowedContenttypes.any { allowed ->
        allowed.contentType.equals(contentType.contentType, ignoreCase = true) &&
            allowed.contentSubtype.equals(contentType.contentSubtype, ignoreCase = true)
    }
}
