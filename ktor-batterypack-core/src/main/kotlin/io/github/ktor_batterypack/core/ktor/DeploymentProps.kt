package io.github.ktor_batterypack.core.ktor

/**
 * HTTP server deployment settings.
 *
 * @property port The port the server listens on. Defaults to 8080.
 */
data class DeploymentProps(
    val port: Int = 8080
)
