package io.github.kperczynski.libs.redis

interface RedisStreamListener {

    fun stream(): String

    suspend fun onMessage(message: String)

    fun group() = RedisStreamListenerGroups.MAIN_GROUP

}

class RedisStreamListenerGroups {
    companion object {
        const val MAIN_GROUP = "main"
        const val TEST_GROUP = "test"
    }
}