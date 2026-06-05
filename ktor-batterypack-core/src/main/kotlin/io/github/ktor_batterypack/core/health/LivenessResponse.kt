package io.github.ktor_batterypack.core.health

data class LivenessResponse(
    val liveness: HealthStatus = HealthStatus.UP
)
