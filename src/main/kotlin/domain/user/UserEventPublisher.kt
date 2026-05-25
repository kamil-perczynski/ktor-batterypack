package io.github.kperczynski.domain.user

import io.github.kperczynski.libs.redis.RedisStreamPublisher
import org.koin.core.annotation.Singleton

const val USER_EVENTS_TOPIC = "user_events"

@Singleton
class UserEventPublisher(private val redisStreamPublisher: RedisStreamPublisher) {

    fun publish(event: UserEvent) {
        redisStreamPublisher.publish(
            stream = USER_EVENTS_TOPIC,
            payload = event,
            headers = mapOf("X-Correlation-Id" to event.userId),
        )
    }
}
