package io.github.ktor_batterypack.redis

import io.github.ktor_batterypack.redis.testing.RedisBatteryIT
import io.lettuce.core.RedisClient
import io.lettuce.core.XAddArgs
import io.lettuce.core.XReadArgs
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.ktor.plugin.koin
import java.util.UUID

class RedisConnectivityBatteryIT : RedisBatteryIT() {

    private val redisClient = application.koin().get<RedisClient>()

    private lateinit var connection: StatefulRedisConnection<String, String>

    @BeforeEach
    fun setUp() {
        connection = redisClient.connect()
    }

    @AfterEach
    fun tearDown() {
        connection.close()
    }

    @Test
    fun `should connect to redis and perform basic operations`() = runTest {
        // given: a redis connection
        val async = connection.async()

        // when: writing and reading a key
        async.set("test:key", "hello-redis").await()
        val value = async.get("test:key").await()

        // then: the value is correctly stored
        assertThat(value).isEqualTo("hello-redis")

        // when: deleting the key
        async.del("test:key").await()
        val deleted = async.get("test:key").await()

        // then: the key no longer exists
        assertThat(deleted).isNull()
    }

    @Test
    fun `should add and read stream messages`() = runBlocking {
        // given: a stream topic with three messages
        val topic = "test_events"
        val payload1 = "event-Monstera-${UUID.randomUUID()}"
        val payload2 = "event-Philodendron-${UUID.randomUUID()}"
        val payload3 = "event-Calathea-${UUID.randomUUID()}"
        val correlationId = UUID.randomUUID().toString()

        val async = connection.async()
        async.xadd(
            topic,
            XAddArgs.Builder.maxlen(128),
            mapOf("_p" to payload1, "X-Correlation-Id" to correlationId)
        ).await()
        async.xadd(
            topic,
            XAddArgs.Builder.maxlen(128),
            mapOf("_p" to payload2)
        ).await()
        async.xadd(
            topic,
            XAddArgs.Builder.maxlen(128),
            mapOf("_p" to payload3)
        ).await()

        // when: reading messages from the stream
        val messages = async
            .xread(XReadArgs.Builder.count(3), XReadArgs.StreamOffset.from(topic, "0"))
            .await()

        // then: all messages are returned with correct payloads and headers
        assertThat(messages).hasSize(3)
        assertThat(messages[0].body["_p"]).isEqualTo(payload1)
        assertThat(messages[0].body["X-Correlation-Id"]).isEqualTo(correlationId)

        Unit
    }
}
