package io.github.kperczynski.libs.redis.monitoring

import io.github.ktor_batterypack.core.health.HealthCheckResult
import io.github.ktor_batterypack.core.health.HealthStatus
import io.github.ktor_batterypack.core.health.ReadinessCheck
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.api.coroutines

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RedisReadinessCheck(private val redisClient: RedisClient) : ReadinessCheck {

    override suspend fun check(): HealthCheckResult {
        return redisClient.connect().use { connection ->
            val pingResult = connection.coroutines().ping()

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
