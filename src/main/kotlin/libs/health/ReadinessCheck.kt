package io.github.kperczynski.libs.health

interface ReadinessCheck {
    fun check(): HealthCheckResult
}
