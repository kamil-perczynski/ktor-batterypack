package io.github.ktor_batterypack.redis.testing

import io.github.ktor_batterypack.redis.RedisStreamListener
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Test helper that captures every payload received on a Redis stream.
 */
open class MsgCapturingRedisListener(private val streamName: String) : RedisStreamListener {

    /** All payloads received by this listener, in arrival order. */
    val payloads = CopyOnWriteArrayList<CapturedMsg>()

    override fun stream(): String = streamName

    override suspend fun onMessage(payload: String, headers: Map<String, String>) {
        payloads.add(CapturedMsg(payload, headers))
    }
}

/**
 * A captured Redis stream message.
 */
data class CapturedMsg(val payload: String, val headers: Map<String, String>)
