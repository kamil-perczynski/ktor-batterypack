package io.github.ktor_batterypack.core.health

/** Result of a single health check, identified by name and status. */
data class HealthCheckResult(
    val name: String,
    val status: HealthStatus
)
