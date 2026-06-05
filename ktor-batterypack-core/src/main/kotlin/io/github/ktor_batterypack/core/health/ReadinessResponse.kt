package io.github.ktor_batterypack.core.health

/** Response returned by the readiness endpoint, containing the overall status and per-check results. */
data class ReadinessResponse(
    val status: HealthStatus,
    val checks: Map<String, HealthStatus>
)
