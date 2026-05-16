@file:OptIn(ExperimentalLettuceCoroutinesApi::class)

package io.github.kperczynski.infra

import io.github.kperczynski.domain.plant.model.Plant
import io.github.kperczynski.domain.somePlant
import io.github.kperczynski.libs.RedisTestContainer
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.XReadArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.core.pubsub.RedisPubSubListener
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.JsonNodeFactory
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


private val log = LoggerFactory.getLogger(RedisConnectivityIT::class.java)

class RedisConnectivityIT {

    companion object {
        private lateinit var jsonMapper: JsonMapper
        private val container = RedisTestContainer()
        private lateinit var redisClient: RedisClient
        private lateinit var connection: StatefulRedisConnection<String, String>
        private lateinit var pubSubConnection: StatefulRedisPubSubConnection<String, String>

        @JvmStatic
        @BeforeAll
        fun setup() {
            container.start()
            redisClient = RedisClient.create(container.redisUri)
            connection = redisClient.connect()
            pubSubConnection = redisClient.connectPubSub()

            jsonMapper = JsonMapper.builder()
                .findAndAddModules()
                .build()
        }

        @JvmStatic
        @AfterAll
        fun teardown() {
            pubSubConnection.close()
            connection.close()
            redisClient.close()
        }
    }

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
    fun `should subscribe and receive pubsub messages`() = runTest {
        val messageDeferred = CompletableDeferred<String>()

        val listener = object : RedisPubSubListener<String, String> {
            override fun message(channel: String, message: String) {
                if (!messageDeferred.isCompleted) {
                    messageDeferred.complete(message)
                    val readValue = jsonMapper.readValue(message, Plant::class.java)
                    log.info("Received message on channel '{}': {}", channel, readValue)
                }
            }

            override fun message(pattern: String, channel: String, message: String) {}
            override fun subscribed(channel: String, count: Long) {
                log.info("Subscribed to channel '$channel', total subscriptions: $count")
            }

            override fun psubscribed(pattern: String, count: Long) {}
            override fun unsubscribed(channel: String, count: Long) {
                log.info("Unsubscribed from channel '$channel', total subscriptions: $count")
            }

            override fun punsubscribed(pattern: String, count: Long) {}
        }

        pubSubConnection.addListener(listener)

        pubSubConnection.async().subscribe("test:channel").await()
        delay(100.milliseconds)


        val plantJson = jsonMapper.writeValueAsString(somePlant())

        connection.coroutines().publish("test:channel", plantJson)

        try {
            val receivedMessage = withTimeoutOrNull(5000.milliseconds) { messageDeferred.await() }
            assertThat(receivedMessage).isEqualTo(plantJson)
        } finally {
            pubSubConnection.async().unsubscribe("test:channel").await()
        }
    }

    @Test
    fun `should add and read stream messages`() = runTest {
        val streamKey = "test:stream"
        val messageDeferred = CompletableDeferred<String>()

        val readJob = launch {
            var lastSeenId = "$"
            redisClient.connect().use { consumerConnection ->
                while (true) {
                    val messages = consumerConnection
                        .async()
                        .xread(
                            XReadArgs.Builder.block(5000).count(10),
                            XReadArgs.StreamOffset.from(streamKey, lastSeenId)
                        )
                        .await()

                    for (message in messages) {
                        log.info("Received message from stream '{}': {}", streamKey, message)
                        lastSeenId = message.id
                    }

                    if (messages.isNotEmpty()) {
                        val payload = messages.first().body["payload"] ?: ""
                        if (payload.contains("foo4")) {
                            messageDeferred.complete(payload)
                        }
                    }
                }
            }
        }

        delay(4.seconds)
        val plantJson = jsonMapper.writeValueAsString(somePlant())
        try {
            connection.coroutines().also {
                it.xadd(
                    streamKey,
                    mapOf(
                        "payload" to JsonNodeFactory.instance.objectNode().put("boo", "foo1")
                            .toString()
                    )
                )
                it.xadd(
                    streamKey,
                    mapOf(
                        "payload" to plantJson
                    )
                )
                it.xadd(
                    streamKey,
                    mapOf(
                        "payload" to JsonNodeFactory.instance.objectNode().put("boo", "foo3")
                            .toString()
                    )
                )
                delay(4.seconds)
                it.xadd(
                    streamKey,
                    mapOf(
                        "payload" to JsonNodeFactory.instance.objectNode().put("boo", "foo4")
                            .toString()
                    )
                )
            }

            val receivedMessage = withTimeoutOrNull(5.seconds) { messageDeferred.await() }
            assertThat(receivedMessage).isEqualTo("{\"boo\":\"foo4\"}")
        } finally {
            readJob.cancel()
            connection.coroutines().del(streamKey)
        }
    }

}
