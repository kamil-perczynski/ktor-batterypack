package io.github.ktor_batterypack.redis

import io.lettuce.core.codec.StringCodec
import io.lettuce.core.output.StatusOutput
import io.lettuce.core.protocol.Command
import io.lettuce.core.protocol.CommandType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

class RedisCommandTimeoutSourceTest {

    private val timeoutSource = RedisCommandTimeoutSource(
        commandTimeout = Duration.ofSeconds(10),
        blockingTimeout = Duration.ofSeconds(5),
    )

    @Test
    fun `should extend timeout of blocking stream reads by the blocking timeout`() {
        val command = Command(CommandType.XREADGROUP, StatusOutput(StringCodec.UTF8))

        assertThat(timeoutSource.getTimeout(command)).isEqualTo(15_000L)
    }

    @Test
    fun `should apply command timeout to regular commands`() {
        val command = Command(CommandType.XACK, StatusOutput(StringCodec.UTF8))

        assertThat(timeoutSource.getTimeout(command)).isEqualTo(10_000L)
    }

    @Test
    fun `should apply command timeout when command is unknown`() {
        assertThat(timeoutSource.getTimeout(null)).isEqualTo(10_000L)
    }

}
