package io.github.kperczynski.domain.plant

import io.lettuce.core.XAddArgs
import io.lettuce.core.api.StatefulRedisConnection
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory
import tools.jackson.databind.json.JsonMapper

private val log = LoggerFactory.getLogger(PlantEventPublisher::class.java)

@Single
class PlantEventPublisher(
    private val connection: StatefulRedisConnection<String, String>,
    private val jsonMapper: JsonMapper
) {

    fun publish(event: PlantEvent) {
        log.info("Publishing plant event: {}", event)

        val publisher = connection.async()
        val eventJson = jsonMapper.writeValueAsString(event)
        publisher.xadd(
            PLANT_EVENTS_TOPIC,
            XAddArgs.Builder.maxlen(128),
            mapOf("_p" to eventJson)
        )
    }

}