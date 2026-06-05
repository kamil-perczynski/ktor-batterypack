package io.github.ktor_batterypack.core.health

data class HealthCheckResult(
    val name: String,
    val status: HealthStatus
)
