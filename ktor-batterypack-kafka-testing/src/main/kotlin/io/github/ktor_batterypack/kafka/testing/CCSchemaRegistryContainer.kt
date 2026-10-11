package io.github.ktor_batterypack.kafka.testing

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
import java.time.Duration

class CCSchemaRegistryContainer(brokerHost: String = "broker") :
    GenericContainer<CCSchemaRegistryContainer>("confluentinc/cp-schema-registry:8.0.3") {

    init {
        withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
        withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "${brokerHost}:29092")
        withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")

        withExposedPorts(8081)

        waitingFor(
            HttpWaitStrategy()
                .forPath("/subjects")
                .forStatusCode(200)
                .withStartupTimeout(Duration.ofSeconds(60))
        )
    }


    val schemaRegistryUrl: String
        get() = "http://127.0.0.1:${this.getMappedPort(8081)}"

}
