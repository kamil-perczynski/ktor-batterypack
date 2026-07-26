package io.github.ktor_batterypack.core.ktor

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import io.github.ktor_batterypack.core.multipart.MultipartProps

/**
 * Top-level configuration properties for a Ktor application.
 */
data class KtorProps(
    @param:JsonPropertyDescription("HTTP server deployment settings")
    val deployment: DeploymentProps = DeploymentProps(),
    @param:JsonPropertyDescription("Application-specific settings")
    val application: KtorApplicationProps = KtorApplicationProps(),
    @param:JsonPropertyDescription("Multipart form handling limits")
    val multipart: MultipartProps = MultipartProps(),
    @param:JsonPropertyDescription("Optional startup banner text")
    val banner: String? = null
)
