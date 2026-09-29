package io.github.ktor_batterypack.redis.monitoring

import io.github.ktor_batterypack.core.health.HealthCheckResult
import io.github.ktor_batterypack.core.health.HealthStatus
import io.github.ktor_batterypack.core.health.ReadinessCheck
import io.github.ktor_batterypack.redis.RedisFacade

class RedisReadinessCheck(private val connectionFacade: RedisFacade) : ReadinessCheck {

    override suspend fun check(): HealthCheckResult {
        return connectionFacade.connect().use { redis ->
            val pingResult = redis.ping()

            if (pingResult == "PONG") {
                HealthCheckResult(
                    name = "redis",
                    status = HealthStatus.UP
                )
            } else {
                HealthCheckResult(
                    name = "redis",
                    status = HealthStatus.DOWN,
                )
            }
        }
    }

}
