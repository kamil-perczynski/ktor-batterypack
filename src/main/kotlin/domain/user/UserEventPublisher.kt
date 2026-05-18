package io.github.kperczynski.domain.user

import io.lettuce.core.XAddArgs
import io.lettuce.core.api.StatefulRedisConnection
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory
import tools.jackson.databind.json.JsonMapper

private val log = LoggerFactory.getLogger(UserEventPublisher::class.java)

@Singleton
class UserEventPublisher(
    private val connection: StatefulRedisConnection<String, String>,
    private val jsonMapper: JsonMapper
) {

    fun publish(event: UserEvent) {
        log.info("Publishing user event: {}", event)

        val publisher = connection.async()
        val eventJson = jsonMapper.writeValueAsString(event)
        publisher.xadd(
            USER_EVENTS_TOPIC,
            XAddArgs.Builder.maxlen(128),
            mapOf("_p" to eventJson, "X-Correlation-Id" to event.userId)
        )
    }

}