package io.github.ktor_batterypack.core.health

data class ReadinessResponse(
    val status: HealthStatus,
    val checks: Map<String, HealthStatus>
)
