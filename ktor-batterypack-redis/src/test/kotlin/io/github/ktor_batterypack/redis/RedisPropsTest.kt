package io.github.ktor_batterypack.redis

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

class RedisPropsTest {

    @Test
    fun `should keep url values when no overrides are set`() {
        val uri = RedisProps(
            url = "rediss://url-user:url-pass@localhost:6379/3?timeout=5s&clientName=url-client"
        ).toRedisUri()

        assertThat(uri.host).isEqualTo("localhost")
        assertThat(uri.port).isEqualTo(6379)
        assertThat(uri.isSsl).isTrue()
        assertThat(uri.database).isEqualTo(3)
        assertThat(uri.clientName).isEqualTo("url-client")
        assertThat(uri.timeout).isEqualTo(Duration.ofSeconds(5))

        val credentials = uri.credentialsProvider.resolveCredentials().block()
        assertThat(credentials.username).isEqualTo("url-user")
        assertThat(String(credentials.password)).isEqualTo("url-pass")
    }

    @Test
    fun `should override url credentials with username and password`() {
        val uri = RedisProps(
            url = "redis://old-user:old-pass@localhost:6379",
            username = "new-user",
            password = "new-pass",
        ).toRedisUri()

        val credentials = uri.credentialsProvider.resolveCredentials().block()
        assertThat(credentials.username).isEqualTo("new-user")
        assertThat(String(credentials.password)).isEqualTo("new-pass")
    }

    @Test
    fun `should authenticate with password only`() {
        val uri = RedisProps(
            url = "redis://localhost:6379",
            password = "secret",
        ).toRedisUri()

        val credentials = uri.credentialsProvider.resolveCredentials().block()
        assertThat(credentials.username).isNull()
        assertThat(String(credentials.password)).isEqualTo("secret")
    }

    @Test
    fun `should override connection settings from the url`() {
        val uri = RedisProps(
            url = "redis://localhost:6379/1?timeout=10s&clientName=url-client",
            ssl = true,
            database = 5,
            clientName = "props-client",
            timeoutMs = 1500,
        ).toRedisUri()

        assertThat(uri.isSsl).isTrue()
        assertThat(uri.database).isEqualTo(5)
        assertThat(uri.clientName).isEqualTo("props-client")
        assertThat(uri.timeout).isEqualTo(Duration.ofMillis(1500))
    }

    @Test
    fun `should disable ssl from the url scheme`() {
        val uri = RedisProps(
            url = "rediss://localhost:6379",
            ssl = false,
        ).toRedisUri()

        assertThat(uri.isSsl).isFalse()
    }

}
