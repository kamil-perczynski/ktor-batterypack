package io.github.kperczynski.libs

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import java.time.Duration

class PostgresTestContainer(image: String = "postgres:17-alpine") :
    GenericContainer<Nothing>(image) {

    init {
        withExposedPorts(5432)
        withEnv("POSTGRES_USER", "ktor")
        withEnv("POSTGRES_PASSWORD", "ktorpassword")
        withEnv("POSTGRES_DB", "ktordb")
        waitingFor(
            LogMessageWaitStrategy()
                .withRegEx(".*database system is ready to accept connections.*\\s")
                .withTimes(2)
                .withStartupTimeout(Duration.ofSeconds(60))
        )
    }

    val jdbcUrl: String
        get() = "jdbc:postgresql://${host}:${getMappedPort(5432)}/ktordb"

    val username: String = "ktor"
    val password: String = "ktorpassword"

}