package io.github.ktor_batterypack.core.health

/** Contract for a readiness check that evaluates a single system component. */
interface ReadinessCheck {
    /** Performs the check and returns the result. */
    suspend fun check(): HealthCheckResult
}
