package io.github.ktor_batterypack.redis.standalone

import io.github.ktor_batterypack.redis.RedisConnectionFacade
import io.github.ktor_batterypack.redis.RedisFacade
import io.lettuce.core.RedisClient

class StandaloneRedisFacade(private val redisClient: RedisClient) : RedisFacade {
    override fun connect(): RedisConnectionFacade =
        StandaloneRedisConnectionFacade(redisClient.connect())

    override fun close() = redisClient.shutdown()
}
