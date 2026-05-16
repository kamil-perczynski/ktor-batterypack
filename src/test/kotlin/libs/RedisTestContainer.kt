package io.github.kperczynski.libs

import org.testcontainers.containers.GenericContainer

class RedisTestContainer(image: String = "redis:7-alpine") :
    GenericContainer<Nothing>(image) {

    init {
        withExposedPorts(6379)
    }

    val redisUri: String
        get() = "redis://$host:${getMappedPort(6379)}"

}
