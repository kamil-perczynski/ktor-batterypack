package io.github.ktor_batterypack.redis

import io.github.ktor_batterypack.core.Closer
import io.github.ktor_batterypack.redis.testing.MsgCapturingRedisListener
import io.github.ktor_batterypack.redis.testing.RedisTestContainer
import io.github.ktor_batterypack.redis_stream.FetcherProps
import io.github.ktor_batterypack.redis_stream.RedisStreamFetcher
import io.github.ktor_batterypack.redis_stream.RedisStreamProps
import io.github.ktor_batterypack.redis_stream.RedisStreamsBackgroundLoop
import io.github.ktor_batterypack.redis_stream.bgloops.RedisStreamAutoclaimLoop
import io.github.ktor_batterypack.redis_stream.bgloops.RedisStreamFetchingLoop
import io.github.ktor_batterypack.redis_stream.bgloops.StreamMessageProcessor
import io.github.ktor_batterypack.redis_stream.monitoring.RedisStreamConsumerLagMonitorLoop
import io.github.ktor_batterypack.redis_stream.monitoring.RedisStreamMetrics
import io.lettuce.core.Consumer
import io.lettuce.core.KillArgs
import io.lettuce.core.XReadArgs
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class RedisStreamReconnectBatteryIT {

    companion object {
        private val redisContainer = RedisTestContainer()

        init {
            redisContainer.start()
        }
    }

    private val consumerGroup = "RedisStreamReconnectBatteryIT"

    private val redisProps = RedisProps(
        url = redisContainer.redisUri,
        timeoutMs = 500,
        streams = RedisStreamProps(
            fetcher = FetcherProps(
                fetchingTimeout = 100,
                fetchingCount = 10,
                autoclaimIntervalMs = 100,
                autoclaimMinIdleMs = 200,
                autoclaimCount = 10,
                lagCheckIntervalMs = 100,
                consumerGroup = consumerGroup
            )
        ),
    )

    private val meterRegistry = SimpleMeterRegistry()
    private val metrics = RedisStreamMetrics(meterRegistry)
    private val redisClient = KtorBatterypackRedisModule().redisClient(redisProps, SimpleMeterRegistry())
    private val messageProcessor = StreamMessageProcessor(metrics)

    private val fetchingLoop = RedisStreamFetchingLoop(redisClient, messageProcessor, metrics, redisProps)
    private val autoclaimLoop = RedisStreamAutoclaimLoop(redisClient, messageProcessor, metrics, redisProps)
    private val lagMonitorLoop = RedisStreamConsumerLagMonitorLoop(redisClient, metrics, redisProps)

    private val closer = Closer()

    init {
        closer.add { redisClient.shutdown() }
    }

    @AfterEach
    fun tearDown() {
        closer.close()
    }

    @Test
    fun `should resume consuming after redis stops responding`() = runBlocking {
        // given: a running fetcher that already consumed a message
        val stream = "test-reconnect-pause-${UUID.randomUUID()}"
        val listener = MsgCapturingRedisListener(stream)
        startFetcher(listener, listOf(fetchingLoop, autoclaimLoop, lagMonitorLoop))

        publish(stream, "before-outage")
        awaitUntil(10.seconds) { listener.payloads.any { it.payload == "before-outage" } }
        val expectedClients = 4
        awaitUntil(10.seconds) { connectedClients() == expectedClients }

        // when: redis stops responding for longer than the command timeout
        pauseRedis(2.seconds)
        publish(stream, "after-outage")

        // then: the fetcher reconnects and consumes the new message
        awaitUntil(20.seconds) { listener.payloads.any { it.payload == "after-outage" } }
        assertThat(connectionFailures("stream-fetching")).isGreaterThan(0.0)

        // and: broken connections are released
        awaitUntil(10.seconds) { connectedClients() == expectedClients }
    }

    @Test
    fun `should resume consuming after the connection is killed`() = runBlocking {
        // given: a running fetcher that already consumed a message
        val stream = "test-reconnect-kill-${UUID.randomUUID()}"
        val listener = MsgCapturingRedisListener(stream)
        startFetcher(listener, listOf(fetchingLoop, autoclaimLoop, lagMonitorLoop))

        publish(stream, "before-kill")
        awaitUntil(10.seconds) { listener.payloads.any { it.payload == "before-kill" } }

        // when: redis drops every client connection
        redisClient.connect().use { it.sync().clientKill(KillArgs.Builder.typeNormal()) }
        publish(stream, "after-kill")

        // then: the fetcher consumes the new message
        awaitUntil(20.seconds) { listener.payloads.any { it.payload == "after-kill" } }
    }

    @Test
    fun `should reclaim pending messages after redis stops responding`() = runBlocking {
        // given: a fetcher running only the autoclaim loop
        val stream = "test-reconnect-autoclaim-${UUID.randomUUID()}"
        val listener = MsgCapturingRedisListener(stream)
        startFetcher(listener, listOf(autoclaimLoop))

        // when: redis stops responding for longer than the command timeout
        pauseRedis(2.seconds)

        // and: a dead consumer leaves a pending message behind
        redisClient.connect().use { connection ->
            connection.sync().xadd(stream, mapOf("_p" to "pending-after-outage"))
            connection.sync().xreadgroup(
                Consumer.from(consumerGroup, "dead-consumer"),
                XReadArgs.StreamOffset.from(stream, ">")
            )
        }

        // then: the autoclaim loop reconnects and reclaims the message
        awaitUntil(20.seconds) { listener.payloads.any { it.payload == "pending-after-outage" } }
        assertThat(connectionFailures("autoclaim")).isGreaterThan(0.0)
        Unit
    }

    @Test
    fun `should recreate consumer group when it disappears`() = runBlocking {
        // given: a running fetcher that already consumed a message
        val stream = "test-reconnect-nogroup-${UUID.randomUUID()}"
        val listener = MsgCapturingRedisListener(stream)
        startFetcher(listener, listOf(fetchingLoop))

        publish(stream, "before-destroy")
        awaitUntil(10.seconds) { listener.payloads.any { it.payload == "before-destroy" } }

        // when: the consumer group is destroyed
        redisClient.connect().use { it.sync().xgroupDestroy(stream, consumerGroup) }
        publish(stream, "after-destroy")

        // then: the fetcher recreates the group and consumes the new message
        awaitUntil(20.seconds) { listener.payloads.any { it.payload == "after-destroy" } }
    }

    private fun startFetcher(listener: MsgCapturingRedisListener, loops: List<RedisStreamsBackgroundLoop>) {
        val fetcher = RedisStreamFetcher(
            consumerId = "consumer-${UUID.randomUUID().toString().take(8)}",
            redisClient = redisClient,
            consumerGroup = consumerGroup,
            autoclaimMinIdleMs = redisProps.streams.fetcher.autoclaimMinIdleMs,
            loops = loops,
            listeners = listOf(listener),
        )
        closer.add { fetcher.close() }
        fetcher.onInit()
    }

    private fun publish(stream: String, payload: String) {
        redisClient.connect().use { it.sync().xadd(stream, mapOf("_p" to payload)) }
    }

    private fun connectedClients(): Int {
        return redisClient.connect().use { connection ->
            connection.sync().clientList().lines().count { it.isNotBlank() }
        }
    }

    private fun connectionFailures(loop: String): Double {
        return meterRegistry
            .find("redis.stream.connection.failures")
            .tag("loop", loop)
            .counters()
            .sumOf { it.count() }
    }

    private suspend fun pauseRedis(duration: Duration) {
        val dockerClient = redisContainer.dockerClient
        dockerClient.pauseContainerCmd(redisContainer.containerId).exec()
        try {
            delay(duration)
        } finally {
            dockerClient.unpauseContainerCmd(redisContainer.containerId).exec()
        }
    }

    private suspend fun awaitUntil(timeout: Duration, condition: () -> Boolean) {
        val deadline = System.nanoTime() + timeout.inWholeNanoseconds
        while (!condition()) {
            if (System.nanoTime() > deadline) {
                throw AssertionError("Condition not met within $timeout")
            }
            delay(50.milliseconds)
        }
    }

}
