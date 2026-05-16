package io.github.kperczynski.libs.redis

import io.github.kperczynski.libs.di.InitCallback
import io.lettuce.core.Consumer
import io.lettuce.core.RedisBusyException
import io.lettuce.core.RedisClient
import io.lettuce.core.XGroupCreateArgs
import io.lettuce.core.XReadArgs
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.slf4j.LoggerFactory
import java.lang.AutoCloseable

private val log = LoggerFactory.getLogger(RedisStreamFetcher::class.java)

class RedisStreamFetcher(
    private val fetcherId: String,
    private val redisClient: RedisClient,
    private val listeners: List<RedisStreamListener>,
    private val streams: List<String>,
    private val consumerGroup: String,
    private val dispatcher: CoroutineDispatcher,
) : InitCallback, AutoCloseable {

    private lateinit var consumerConnection: StatefulRedisConnection<String, String>
    private var job: Job? = null

    override fun onInit() {
        this.consumerConnection = redisClient.connect()

        runBlocking {
            log.info(
                "Registering {} redis stream listener(s) in group: {}, streams: {}",
                listeners.size,
                consumerGroup,
                streams
            )
            for (streamKey in streams) {
                try {
                    consumerConnection.async()
                        .xgroupCreate(
                            XReadArgs.StreamOffset.from(streamKey, "0"),
                            consumerGroup,
                            XGroupCreateArgs().mkstream(true)
                        )
                        .await()
                } catch (e: RedisBusyException) {
                    log.warn(
                        "Consumer group {} already exists for stream {}, skipping group creation",
                        consumerGroup,
                        streamKey
                    )
                    log.trace("Existing consumer group error details", e)
                }
            }
        }
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        job = scope.launch(dispatcher + CoroutineName("RedisStreamFetcher-$fetcherId")) {
            val consumer = Consumer.from(consumerGroup, fetcherId)
            val offsets = streams.map { XReadArgs.StreamOffset.from(it, ">") }.toTypedArray()

            val listenersIdx = listeners.associateBy { it.stream() }

            while (true) {
                val messages = consumerConnection
                    .async()
                    .xreadgroup(
                        consumer,
                        XReadArgs.Builder.block(5000).count(10),
                        *offsets
                    )
                    .await()

                for (message in messages) {
                    log.debug("Received redis stream message: {}", message)
                    val payload = message.body["_p"] ?: ""
                    try {
                        listenersIdx[message.stream]?.onMessage(payload)
                    } catch (e: Exception) {
                        log.error(
                            "Error processing message from stream {} with id {}: {}",
                            message.stream,
                            message.id,
                            e.message,
                            e
                        )
                    } finally {
                        consumerConnection
                            .async()
                            .xack(message.stream, consumerGroup, message.id)
                            .await()
                    }

                }
            }
        }
    }

    override fun close() {
        log.info("Closing RedisStreamFetcher: {}", fetcherId)
        runBlocking {
            job?.cancelAndJoin()
            consumerConnection.close()
        }
    }

}
