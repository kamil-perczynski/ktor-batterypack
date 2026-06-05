package io.github.kperczynski.domain.user

import io.github.ktor_batterypack.redis.RedisStreamListener
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory
import tools.jackson.databind.json.JsonMapper

private val log = LoggerFactory.getLogger(UserEventsListener::class.java)

@Singleton
class UserEventsListener(private val jsonMapper: JsonMapper) : RedisStreamListener {
    override fun stream(): String = USER_EVENTS_TOPIC

    override suspend fun onMessage(payload: String, headers: Map<String, String>) {
        val event = jsonMapper.readValue(payload, UserEvent::class.java)
        log.info("Received user event: {}, headers: {}", event, headers)
    }
}
