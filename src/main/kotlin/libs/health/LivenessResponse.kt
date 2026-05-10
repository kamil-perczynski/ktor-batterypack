package io.github.kperczynski.libs.health

data class LivenessResponse(
    val liveness: HealthStatus = HealthStatus.UP
)
