package io.github.kperczynski.infra.health

import io.github.kperczynski.libs.health.HealthCheckResult
import io.github.kperczynski.libs.health.HealthStatus
import io.github.kperczynski.libs.health.ReadinessCheck
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
