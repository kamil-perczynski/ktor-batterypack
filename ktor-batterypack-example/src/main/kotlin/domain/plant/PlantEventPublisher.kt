package io.github.kperczynski.domain.plant

import io.github.ktor_batterypack.redis.RedisStreamPublisher
import org.koin.core.annotation.Singleton

const val PLANT_EVENTS_TOPIC = "plant_events"

@Singleton
class PlantEventPublisher(
    private val redisStreamPublisher: RedisStreamPublisher,
) {

    fun publish(event: PlantEvent) {
        redisStreamPublisher.publish(
            stream = PLANT_EVENTS_TOPIC,
            payload = event,
            headers = mapOf("X-Correlation-Id" to event.plantId),
        )
    }
}
