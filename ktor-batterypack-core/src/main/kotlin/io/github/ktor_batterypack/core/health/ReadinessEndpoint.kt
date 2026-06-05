package io.github.ktor_batterypack.core.health

/** Aggregates all readiness checks and produces a combined readiness response. */
class ReadinessEndpoint(private val checks: List<ReadinessCheck>) {

    /** Runs all checks and returns a combined readiness response. */
    suspend fun check(): ReadinessResponse {
        val results = checks.map { it.check() }
        val isUp = results.all { it.status == HealthStatus.UP }

        val status = if (isUp) {
            HealthStatus.UP
        } else {
            HealthStatus.DOWN
        }

        val checksMap = results.associate { it.name to it.status }

        return ReadinessResponse(
            status = status,
            checks = checksMap
        )
    }

}
