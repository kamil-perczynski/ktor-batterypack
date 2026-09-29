package io.github.ktor_batterypack.redis

interface RedisStreamListener {

    suspend fun onMessage(payload: String, headers: Map<String, String> = emptyMap())

    fun config(): RedisStreamListenerConfig

}

data class RedisStreamListenerConfig(
    val streamId: String,
    val consumerGroup: String? = null,
)
