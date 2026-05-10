package io.github.kperczynski.libs.health

data class HealthCheckResult(
    val name: String,
    val status: HealthStatus
)
