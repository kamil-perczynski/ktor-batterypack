package io.github.kperczynski.libs.redis.bgloops

import io.github.kperczynski.libs.redis.RedisProps
import io.github.kperczynski.libs.redis.RedisStreamListener
import io.github.kperczynski.libs.redis.RedisStreamsBackgroundLoop
import io.lettuce.core.Consumer
import io.lettuce.core.RedisClient
import io.lettuce.core.XReadArgs
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.koin.core.annotation.Factory
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds

private val log = LoggerFactory.getLogger(RedisStreamFetchingLoop::class.java)

@Factory
class RedisStreamFetchingLoop(
    private val redisClient: RedisClient,
    private val messageProcessor: StreamMessageProcessor,
    private val redisProps: RedisProps,
) : RedisStreamsBackgroundLoop {

    private lateinit var connection: StatefulRedisConnection<String, String>
    private lateinit var job: Job

    private val scope: CoroutineScope =
        CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineName("StreamFetching"))

    override fun init() {
        connection = redisClient.connect()
    }

    override fun close() {
        runBlocking {
            scope.cancel()
            scope.coroutineContext.job.join()
        }
        if (connection.isOpen) {
            log.debug("Closing Redis connection for stream fetching loop")
            connection.close()
        }
    }

    override fun start(
        fetcherId: String,
        listeners: Map<String, RedisStreamListener>,
        consumerGroup: String,
    ) {
        val fetchingTimeout = redisProps.fetcher.fetchingTimeout
        val fetchingCount = redisProps.fetcher.fetchingCount
        val streams = listeners.keys.toList()
        val consumer = Consumer.from(consumerGroup, fetcherId)

        job = scope.launch {
            val offsets = streams.map { XReadArgs.StreamOffset.from(it, ">") }.toTypedArray()

            while (isActive) {
                try {
                    val messages = connection
                        .async()
                        .xreadgroup(
                            consumer,
                            XReadArgs.Builder.block(fetchingTimeout).count(fetchingCount),
                            *offsets
                        )
                        .await()

                    messageProcessor.processMessages(
                        messages = messages,
                        listeners = listeners,
                        consumerGroup = consumerGroup,
                        connection = connection
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.error(
                        "Redis xreadgroup failed, retrying after {}ms: {}",
                        fetchingTimeout,
                        e.message,
                        e
                    )
                    delay(fetchingTimeout.milliseconds)
                }
            }
        }
    }
}
