package io.github.kperczynski.libs.redis

import io.lettuce.core.XAddArgs
import io.lettuce.core.api.StatefulRedisConnection
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory
import tools.jackson.databind.json.JsonMapper
import kotlin.time.Duration

private val log = LoggerFactory.getLogger(RedisStreamPublisher::class.java)

@Singleton
class RedisStreamPublisher(
    private val connection: StatefulRedisConnection<String, String>,
    private val jsonMapper: JsonMapper,
    private val redisProps: RedisProps,
) {

    fun publish(
        stream: String,
        payload: Any,
        headers: Map<String, String> = emptyMap(),
        retentionDuration: Duration? = null
    ) {
        log.debug("Publishing to stream '{}': {}", stream, payload)

        val publisher = connection.async()
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
