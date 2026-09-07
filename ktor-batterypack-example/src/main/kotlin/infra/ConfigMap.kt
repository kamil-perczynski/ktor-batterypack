package io.github.kperczynski.infra

import io.github.kperczynski.infra.client.FlorinClientProps
import io.github.ktor_batterypack.database.DatabaseProps
import io.github.ktor_batterypack.redis.RedisProps
import io.github.ktor_batterypack.core.ktor.KtorProps

/**
 * Root configuration aggregating all application properties.
 */
data class ConfigMap(
    /** Ktor server configuration */
    val ktor: KtorProps = KtorProps(),
    /** Database connection and pool configuration */
    val database: DatabaseProps = DatabaseProps(),
    /** Florin HTTP client configuration */
    val florin: FlorinClientProps = FlorinClientProps(),
    /** Redis connection and stream configuration */
    val redis: RedisProps = RedisProps(),
)


