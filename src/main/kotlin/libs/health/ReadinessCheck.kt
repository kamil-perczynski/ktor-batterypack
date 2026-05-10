package io.github.kperczynski.libs.health

interface ReadinessCheck {
    suspend fun check(): HealthCheckResult
}
