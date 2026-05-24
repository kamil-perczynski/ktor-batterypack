package io.github.kperczynski.infra

import io.github.kperczynski.libs.Closer
import io.github.kperczynski.libs.redis.RedisStreamFetcher
import io.github.kperczynski.libs.redis.RedisStreamListener
import io.github.kperczynski.libs.redis.RedisStreamMetrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.lettuce.core.Consumer
import io.lettuce.core.RedisClient
import io.lettuce.core.XGroupCreateArgs
import io.lettuce.core.XReadArgs
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.ktor.plugin.koin
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.milliseconds

class RedisStreamFetcherIT : KtorBatteriesIT() {

    private val redisClient: RedisClient = application.koin().get()

    private val connection = redisClient.connect()
    private val async get() = connection.async()
    private val closer = Closer()

    @BeforeEach
    fun setUp() {
        closer.add { connection.close() }
    }

    @AfterEach
    fun tearDown() {
        closer.close()
    }

    @Test
    fun `should autoclaim messages from dead consumers`() = runBlocking {
        // given: a stream with a message claimed by a dead consumer
        val stream = "test-autoclaim-${UUID.randomUUID()}"
        val group = "test-autoclaim-group"
        val payload = "test-payload-${UUID.randomUUID()}"

        async.xadd(stream, mapOf("_p" to payload)).await()
        async
            .xgroupCreate(
                XReadArgs.StreamOffset.from(stream, "0"),
                group,
                XGroupCreateArgs().mkstream(true)
            )
            .await()

        val deadConsumer = Consumer.from(group, "dead-consumer")
        val claimed = async
            .xreadgroup(
                deadConsumer,
                XReadArgs.StreamOffset.from(stream, ">")
            )
            .await()

        assertThat(claimed).hasSize(1)
        assertThat(claimed[0].body["_p"]).isEqualTo(payload)

        // when: a new fetcher starts with autoclaim enabled
        val listener = TestListener(stream)
        val fetcher = RedisStreamFetcher(
            fetcherId = "reclaimer",
            redisClient = redisClient,
            listeners = listOf(listener),
            consumerGroup = group,
            fetchingTimeout = 100,
            fetchingCount = 10,
            autoclaimIntervalMs = 100,
            autoclaimMinIdleMs = 200,
            autoclaimCount = 10,
            lagCheckIntervalMs = 1000,
            metrics = RedisStreamMetrics(SimpleMeterRegistry())
        )
        closer.add { fetcher.close() }

        fetcher.onInit()
        delay(250.milliseconds)

        // then: the message is reclaimed and delivered
        assertThat(listener.payloads).containsExactly(payload)
        Unit
    }

    @Test
    fun `should cleanup inactive consumers on init`() = runBlocking {
        // given: a stream with an old inactive consumer
        val stream = "test-cleanup-${UUID.randomUUID()}"
        val group = "test-cleanup-group"

        async.xadd(stream, mapOf("_p" to "dummy")).await()
        async.xgroupCreate(
            XReadArgs.StreamOffset.from(stream, "0"),
            group,
            XGroupCreateArgs().mkstream(true)
        ).await()

        async.xgroupCreateconsumer(stream, Consumer.from(group, "old-consumer")).await()

        // when: a new fetcher starts up
        val fetcher = RedisStreamFetcher(
            fetcherId = "new-consumer",
            redisClient = redisClient,
            listeners = listOf(TestListener(stream)),
            consumerGroup = group,
            fetchingTimeout = 100,
            fetchingCount = 10,
            autoclaimIntervalMs = 100,
            autoclaimMinIdleMs = 200,
            autoclaimCount = 10,
            lagCheckIntervalMs = 1000,
            metrics = RedisStreamMetrics(SimpleMeterRegistry())
        )
        closer.add { fetcher.close() }
        fetcher.onInit()

        // then: the old consumer is removed from the group
        val raw = async.xinfoConsumers(stream, group).await() as List<*>
        val consumerNames = raw.mapNotNull { (it as? Map<*, *>)?.get("name") as? String }
        assertThat(consumerNames).doesNotContain("old-consumer")

        Unit
    }

    private class TestListener(private val streamName: String) : RedisStreamListener {
        val payloads = CopyOnWriteArrayList<String>()

        override fun stream(): String = streamName

        override suspend fun onMessage(payload: String, headers: Map<String, String>) {
            payloads.add(payload)
        }
    }
}
