package io.github.ktor_batterypack.redis

import io.lettuce.core.TimeoutOptions
import io.lettuce.core.protocol.CommandType
import io.lettuce.core.protocol.RedisCommand
import java.time.Duration

class RedisCommandTimeoutSource(
    private val commandTimeout: Duration,
    private val blockingTimeout: Duration,
) : TimeoutOptions.TimeoutSource() {

    override fun getTimeout(command: RedisCommand<*, *, *>?): Long {
        val type = command?.type
        if (type == CommandType.XREADGROUP || type == CommandType.XREAD) {
            return commandTimeout.plus(blockingTimeout).toMillis()
        }
        return commandTimeout.toMillis()
    }

}
