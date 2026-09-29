package io.github.ktor_batterypack.redis

import io.lettuce.core.api.async.RedisStreamAsyncCommands
import io.lettuce.core.api.sync.RedisStreamCommands

interface RedisConnectionFacade : AutoCloseable {
    val isOpen: Boolean
    val stream: RedisStreamCommands<String, String>
    val streamAsync: RedisStreamAsyncCommands<String, String>
    fun ping(): String
}
