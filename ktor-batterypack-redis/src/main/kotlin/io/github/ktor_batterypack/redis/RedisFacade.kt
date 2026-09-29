package io.github.ktor_batterypack.redis

interface RedisFacade : AutoCloseable {
    fun connect(): RedisConnectionFacade
}
