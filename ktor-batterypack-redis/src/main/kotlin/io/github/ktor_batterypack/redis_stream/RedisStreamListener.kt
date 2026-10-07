package io.github.ktor_batterypack.redis_stream

/**
 * The extension point for subscribing application code to Redis streams. Implement this
 * interface and register the implementation as a bean; the module discovers every
 * implementation and delivers stream messages to [onMessage].
 */
interface RedisStreamListener {

    suspend fun onMessage(payload: String, headers: Map<String, String> = emptyMap())

    fun config(): RedisStreamListenerConfig

}

/**
 * Declares which stream a [RedisStreamListener] subscribes to and, optionally, which
 * consumer group it belongs to.
 */
data class RedisStreamListenerConfig(
    val streamId: String,
    val consumerGroup: String? = null,
)
