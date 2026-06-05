package io.github.ktor_batterypack.core.ktor

import io.github.ktor_batterypack.core.multipart.MultipartProps

/**
 * Top-level configuration properties for a Ktor application.
 *
 * @property deployment HTTP server deployment settings.
 * @property application Application-specific settings.
 * @property multipart Multipart form handling limits.
 * @property banner Optional startup banner text.
 */
data class KtorProps(
    val deployment: DeploymentProps = DeploymentProps(),
    val application: KtorApplicationProps = KtorApplicationProps(),
    val multipart: MultipartProps = MultipartProps(),
    val banner: String? = null
)
