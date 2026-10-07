package io.github.ktor_batterypack.redis

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import io.github.ktor_batterypack.redis_stream.RedisStreamProps

/**
 * Redis connection and stream configuration properties.
 */
data class RedisProps(
    @param:JsonPropertyDescription("Redis connection URL")
    val url: String = "redis://localhost:6379",
    @param:JsonPropertyDescription("Redis ACL username (Redis 6+); overrides credentials embedded in the URL")
    val username: String? = null,
    @param:JsonPropertyDescription("Redis password; overrides credentials embedded in the URL")
    val password: String? = null,
    @param:JsonPropertyDescription("Use TLS for the connection; overrides the rediss:// scheme")
    val ssl: Boolean? = null,
    @param:JsonPropertyDescription("Redis database number; overrides the database in the URL")
    val database: Int? = null,
    @param:JsonPropertyDescription("Client name reported to Redis (visible in CLIENT LIST); overrides the clientName URL parameter")
    val clientName: String? = null,
    @param:JsonPropertyDescription("Command timeout in milliseconds; overrides the timeout URL parameter")
    val timeoutMs: Long? = null,
    @param:JsonPropertyDescription("Redis streams props")
    val streams: RedisStreamProps = RedisStreamProps(),
)
