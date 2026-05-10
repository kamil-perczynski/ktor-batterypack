package io.github.kperczynski.libs.health

data class ReadinessResponse(
    val status: HealthStatus,
    val checks: Map<String, HealthStatus>
)
