package io.github.ktor_batterypack.core.health

interface ReadinessCheck {
    suspend fun check(): HealthCheckResult
}
