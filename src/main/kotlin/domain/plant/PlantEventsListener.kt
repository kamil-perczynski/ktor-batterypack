package io.github.kperczynski.domain.plant

import io.github.kperczynski.libs.redis.RedisStreamListener
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory
import tools.jackson.databind.json.JsonMapper

private val log = LoggerFactory.getLogger(PlantEventsListener::class.java)

const val PLANT_EVENTS_TOPIC = "plant_events"

@Singleton
class PlantEventsListener(private val jsonMapper: JsonMapper) : RedisStreamListener {
    override fun stream(): String = PLANT_EVENTS_TOPIC

    override suspend fun onMessage(payload: String, headers: Map<String, String>) {
        val event = jsonMapper.readValue(payload, PlantEvent::class.java)
        log.info("Received plant event: {}, headers: {}", event, headers)
    }
}

data class PlantEvent(
    val plantId: String,
    val type: PlantEventType,
    val externalId: String,
)

enum class PlantEventType {
    PLANT_READ,
    PLANT_CREATED,
    PLANT_UPDATED,
    PLANT_DELETED
}