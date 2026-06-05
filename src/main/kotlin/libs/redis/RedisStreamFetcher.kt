package io.github.kperczynski.libs.redis

import io.github.ktor_batterypack.core.di.InitCallback
import io.github.kperczynski.libs.redis.bgloops.toXInfoResultDto
import io.lettuce.core.Consumer
import io.lettuce.core.RedisBusyException
import io.lettuce.core.RedisClient
import io.lettuce.core.XGroupCreateArgs
import io.lettuce.core.XReadArgs
import io.lettuce.core.api.StatefulRedisConnection
import org.slf4j.LoggerFactory
import java.lang.AutoCloseable

private val log = LoggerFactory.getLogger(RedisStreamFetcher::class.java)

class RedisStreamFetcher(
    private val consumerId: String,
    private val redisClient: RedisClient,
    private val consumerGroup: String,
    private val autoclaimMinIdleMs: Long,
    private val loops: List<RedisStreamsBackgroundLoop>,
    listeners: List<RedisStreamListener>,
) : InitCallback, AutoCloseable {

    private val listenersIdx: Map<String, RedisStreamListener> =
        listeners.associateBy { it.stream() }

    private val streams = listeners.map { it.stream() }.distinct()

    private val handles = mutableListOf<LoopHandle>()

    override fun onInit() {
        redisClient.connect().use { setupConnection ->
            cleanupInactiveConsumers(setupConnection, streams)
            createConsumerGroups(setupConnection, streams)
        }

        for (loop in loops) {
            handles += loop.start(
                fetcherId = consumerId,
                listeners = listenersIdx,
                consumerGroup = consumerGroup,
            )
        }
    }

    private fun cleanupInactiveConsumers(
        connection: StatefulRedisConnection<String, String>,
        streams: List<String>
    ) {
        for (stream in streams) {
            try {
                val raw = try {
                    connection
                        .sync()
                        .xinfoConsumers(stream, consumerGroup)
                } catch (e: Exception) {
                    log.debug(
                        "Failed to fetch consumer info for stream {}: {}, skipping cleanup",
                        stream,
                        e.message
                    )
                    null
                }

                if (raw == null) {
                    continue
                }

                val consumers = toXInfoResultDto(raw)

                for (info in consumers) {
                    if (info.name != consumerId && info.pending == 0L && info.idle > autoclaimMinIdleMs) {
                        connection
                            .sync()
                            .xgroupDelconsumer(stream, Consumer.from(consumerGroup, info.name))
                        log.debug("Cleaned up inactive consumer: {} from stream: {}", info.name, stream)
                    }
                }
            } catch (e: Exception) {
                log.warn(
                    "Failed to cleanup inactive consumers for stream {}: {}",
                    stream,
                    e.message,
                    e
                )
            }
        }
    }

    private fun createConsumerGroups(
        connection: StatefulRedisConnection<String, String>,
        streams: List<String>
    ) {
        log.info(
            "Registering {} redis stream listener(s) in group: {}, streams: {}",
            listenersIdx.size,
            consumerGroup,
            streams
        )

        for (streamKey in streams) {
            try {
                connection.sync()
                    .xgroupCreate(
                        XReadArgs.StreamOffset.from(streamKey, "0"),
                        consumerGroup,
                        XGroupCreateArgs().mkstream(true)
                    )
            } catch (e: RedisBusyException) {
                log.debug(
                    "Consumer group {} already exists for stream {}, skipping group creation",
                    consumerGroup,
                    streamKey
                )
                log.trace("Existing consumer group error details", e)
            }
        }
    }

    override fun close() {
        log.debug("Closing RedisStreamFetcher: {}", consumerId)
        for (handle in handles) {
            handle.close()
        }
        log.info("RedisStreamFetcher: {} closed", consumerId)
    }
}
