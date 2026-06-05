package io.github.ktor_batterypack.core.health

/** Response returned by the liveness endpoint indicating the service is alive. */
data class LivenessResponse(
    val liveness: HealthStatus = HealthStatus.UP
)
