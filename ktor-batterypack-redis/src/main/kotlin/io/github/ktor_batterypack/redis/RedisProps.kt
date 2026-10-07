package io.github.ktor_batterypack.redis

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import io.github.ktor_batterypack.redis_stream.RedisStreamProps

/**
 * Redis connection and stream configuration properties.
 */
data class RedisProps(
    @param:JsonPropertyDescription("Redis connection URL")
    val url: String = "redis://localhost:6379",
    @param:JsonPropertyDescription("Redis streams props")
    val streams: RedisStreamProps = RedisStreamProps(),
)
