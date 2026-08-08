package io.github.ktor_batterypack.core.ktor

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

/**
 * HTTP server deployment settings.
 */
data class DeploymentProps(
    @param:JsonPropertyDescription("The port the server listens on")
    @field:Min(1024)
    @field:Max(65535)
    val port: Int = 8080
)
