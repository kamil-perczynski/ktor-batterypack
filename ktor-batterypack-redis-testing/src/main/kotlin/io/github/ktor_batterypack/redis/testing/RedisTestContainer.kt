package io.github.ktor_batterypack.redis.testing

import org.testcontainers.containers.GenericContainer

class RedisTestContainer(image: String = "redis:8-alpine") :
    GenericContainer<Nothing>(image) {

    init {
        withExposedPorts(6379)
    }

    val redisUri: String
        get() = "redis://$host:${getMappedPort(6379)}"

}
