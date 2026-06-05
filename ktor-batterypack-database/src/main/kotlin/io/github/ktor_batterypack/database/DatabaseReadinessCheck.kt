package io.github.ktor_batterypack.database

import io.github.ktor_batterypack.core.health.HealthCheckResult
import io.github.ktor_batterypack.core.health.HealthStatus
import io.github.ktor_batterypack.core.health.ReadinessCheck
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val log = LoggerFactory.getLogger(DatabaseReadinessCheck::class.java)

class DatabaseReadinessCheck(private val dataSource: DataSource) : ReadinessCheck {

    override suspend fun check(): HealthCheckResult {
        val status = try {
            dataSource.connection.use { conn ->
                conn.prepareStatement("SELECT 1").use { stmt ->
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            HealthStatus.UP
                        } else {
                            HealthStatus.DOWN
                        }
                    }
                }
            }
        } catch (e: Exception) {
            log.warn("Database health check failed", e)
            HealthStatus.DOWN
        }

        return HealthCheckResult(
            name = "database",
            status = status
        )
    }
}
