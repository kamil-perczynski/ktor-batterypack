package io.github.ktor_batterypack.redis

interface RedisStreamListener {

    fun stream(): String

    suspend fun onMessage(payload: String, headers: Map<String, String> = emptyMap())

    fun group() = RedisStreamListenerGroups.MAIN_GROUP

}

class RedisStreamListenerGroups {
    companion object {
        const val MAIN_GROUP = "main"
        const val TEST_GROUP = "test"
    }
}
