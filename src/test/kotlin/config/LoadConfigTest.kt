package io.github.kperczynski.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LoadConfigTest {

    @Test
    fun `should load default configuration`() {
        val config = loadConfig()

        assertThat(config.ktor.deployment.port).isEqualTo(8080)
        assertThat(config.database.url).isEqualTo("jdbc:postgresql://localhost:5432/ktordb")
        assertThat(config.database.user).isEqualTo("ktor")
        assertThat(config.database.driver).isEqualTo("org.postgresql.Driver")
        assertThat(config.database.poolSize).isEqualTo(10)
    }

}
