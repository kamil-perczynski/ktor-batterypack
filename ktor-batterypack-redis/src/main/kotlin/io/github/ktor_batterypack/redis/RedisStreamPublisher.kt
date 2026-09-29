package io.github.ktor_batterypack.redis

import io.lettuce.core.XAddArgs
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory
import tools.jackson.databind.json.JsonMapper
import kotlin.time.Duration

private val log = LoggerFactory.getLogger(RedisStreamPublisher::class.java)

@Singleton
class RedisStreamPublisher(
    private val connectionFacade: RedisFacade,
    private val jsonMapper: JsonMapper,
    @Provided private val redisProps: RedisProps,
) : AutoCloseable {

    private val redis = connectionFacade.connect()

    override fun close() {
        if (redis.isOpen) {
            redis.close()
        }
    }

    fun publish(
        stream: String,
        payload: Any,
        headers: Map<String, String> = emptyMap(),
        retentionDuration: Duration? = null
    ) {
        log.debug("Publishing to stream '{}': {}", stream, payload)

        val publisher = redis.streamAsync
        val eventJson = jsonMapper.writeValueAsString(payload)

        val effectiveRetentionMs =
            retentionDuration?.inWholeMilliseconds ?: redisProps.publisher.retentionMs
        val minId = System.currentTimeMillis() - effectiveRetentionMs

        val body = mutableMapOf("_p" to eventJson)
        body.putAll(headers)

        publisher.xadd(
            stream,
            XAddArgs.Builder.minId(minId.toString()).approximateTrimming(),
            body
        )
    }
}
