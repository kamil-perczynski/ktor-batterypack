package io.github.ktor_batterypack.redis.cluster

import io.github.ktor_batterypack.redis.RedisConnectionFacade
import io.github.ktor_batterypack.redis.RedisFacade
import io.lettuce.core.cluster.RedisClusterClient

class ClusterRedisFacade(
    private val redisClient: RedisClusterClient,
) : RedisFacade {
    override fun connect(): RedisConnectionFacade =
        ClusterRedisConnectionFacade(redisClient.connect())

    override fun close() = redisClient.shutdown()
}
