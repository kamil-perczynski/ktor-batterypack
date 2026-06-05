package io.github.ktor_batterypack.core.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LoadConfigTest {

    @Test
    fun `should load default configuration`() {
        val config = loadConfig<TestConfig>(emptyList(), false)

        assertThat(config.app.name).isEqualTo("test-app")
        assertThat(config.app.port).isEqualTo(8080)
        assertThat(config.database.url).isEqualTo("jdbc:postgresql://localhost:5432/ktordb")
        assertThat(config.database.user).isEqualTo("ktor")
        assertThat(config.database.driver).isEqualTo("org.postgresql.Driver")
        assertThat(config.database.poolSize).isEqualTo(2)
    }

}

