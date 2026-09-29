package io.github.ktor_batterypack.redis.cluster

import io.github.ktor_batterypack.redis.RedisConnectionFacade
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection

internal class ClusterRedisConnectionFacade(
    private val connection: StatefulRedisClusterConnection<String, String>,
) : RedisConnectionFacade {
    override val isOpen: Boolean get() = connection.isOpen
    override val stream = connection.sync()
    override val streamAsync = connection.async()
    override fun ping(): String = connection.sync().ping()
    override fun close() = connection.close()
}
