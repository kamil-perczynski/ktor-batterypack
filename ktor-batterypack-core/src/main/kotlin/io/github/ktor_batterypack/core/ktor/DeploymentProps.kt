package io.github.ktor_batterypack.core.ktor

import com.fasterxml.jackson.annotation.JsonPropertyDescription

/**
 * HTTP server deployment settings.
 */
data class DeploymentProps(
    @param:JsonPropertyDescription("The port the server listens on")
    val port: Int = 8080
)
