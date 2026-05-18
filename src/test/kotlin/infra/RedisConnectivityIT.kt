package io.github.kperczynski.infra

import io.github.kperczynski.domain.plant.PLANT_EVENTS_TOPIC
import io.github.kperczynski.domain.plant.PlantEvent
import io.github.kperczynski.domain.plant.PlantEventType
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.XAddArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.koin.ktor.plugin.koin
import tools.jackson.databind.json.JsonMapper
import java.util.Map
import java.util.UUID

@ExperimentalLettuceCoroutinesApi
class RedisConnectivityIT : KtorBatteriesIT() {

    private val jsonMapper: JsonMapper = application.koin().get()
    private val connection: StatefulRedisConnection<String, String> = application.koin().get()
    private val messageCollector: PlantEventsMessageCollector = application.koin().get()

    @Test
    fun `should connect to redis and perform basic operations`() = runTest {
        val async = connection.async()
        async.set("test:key", "hello-redis").await()
        val value = async.get("test:key").await()
        assertThat(value).isEqualTo("hello-redis")

        async.del("test:key").await()
        val deleted = async.get("test:key").await()
        assertThat(deleted).isNull()
    }

    @Test
    fun `should add and read stream messages`() = runBlocking {
        messageCollector.expectResult()

        val event1 = PlantEvent(
            plantId = "Monstera",
            type = PlantEventType.PLANT_CREATED,
            externalId = UUID.randomUUID().toString()
        )
        val event2 = PlantEvent(
            plantId = "Philodendron",
            type = PlantEventType.PLANT_UPDATED,
            externalId = UUID.randomUUID().toString()
        )
        val event3 = PlantEvent(
            plantId = "Calathea",
            type = PlantEventType.PLANT_DELETED,
            externalId = UUID.randomUUID().toString()
        )

        val correlationId = UUID.randomUUID().toString()

        connection.coroutines().also {
            it.xadd(
                PLANT_EVENTS_TOPIC,
                XAddArgs.Builder.maxlen(128),
                mapOf(
                    "_p" to jsonMapper.writeValueAsString(event1),
                    "X-Correlation-Id" to correlationId
                )
            )
            it.xadd(
                PLANT_EVENTS_TOPIC,
                XAddArgs.Builder.maxlen(128),
                mapOf("_p" to jsonMapper.writeValueAsString(event2))
            )
            it.xadd(
                PLANT_EVENTS_TOPIC,
                XAddArgs.Builder.maxlen(128),
                mapOf("_p" to jsonMapper.writeValueAsString(event3))
            )
        }

        val receivedMessage = messageCollector.lastMessage()

        assertThat(receivedMessage?.payload).isEqualTo(
            jsonMapper.writeValueAsString(event1)
        )
        assertThat(receivedMessage?.headers).containsExactly(
            Map.entry("X-Correlation-Id", correlationId)
        )

        Unit
    }

}
