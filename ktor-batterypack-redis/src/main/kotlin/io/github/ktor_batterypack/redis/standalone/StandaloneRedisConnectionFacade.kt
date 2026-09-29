package io.github.ktor_batterypack.redis.standalone

import io.github.ktor_batterypack.redis.RedisConnectionFacade
import io.lettuce.core.api.StatefulRedisConnection

internal class StandaloneRedisConnectionFacade(
    private val connection: StatefulRedisConnection<String, String>,
) : RedisConnectionFacade {
    override val isOpen: Boolean get() = connection.isOpen
    override val stream = connection.sync()
    override val streamAsync = connection.async()
    override fun ping(): String = connection.sync().ping()
    override fun close() = connection.close()
}
