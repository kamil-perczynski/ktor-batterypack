package io.github.ktor_batterypack.redis_stream

import io.github.ktor_batterypack.core.di.InitCallback
import io.github.ktor_batterypack.redis.RedisProps
import io.lettuce.core.RedisClient
import java.lang.AutoCloseable

/**
 * The single lifecycle entry point for Redis stream consumption: registered once as an
 * [InitCallback]/[AutoCloseable] so the framework starts and stops all stream consumers,
 * regardless of how many consumer groups are actually in play.
 */
class RedisStreamFetchers(
    allListeners: List<RedisStreamListener>,
    private val redisProps: RedisProps,
    private val redisClient: RedisClient,
    private val loops: List<RedisStreamsBackgroundLoop>
) : InitCallback, AutoCloseable {

    private var fetchers: List<RedisStreamFetcher>

    init {
        val groupedListeners = allListeners.groupBy {
            it.config().consumerGroup ?: redisProps.streams.fetcher.consumerGroup
        }

        this.fetchers = groupedListeners.map { (consumerGroup, listeners) ->
            RedisStreamFetcher(
                consumerId = nextConsumerId(consumerGroup),
                listeners = listeners,
                consumerGroup = consumerGroup,
                redisClient = redisClient,
                loops = loops,
                autoclaimMinIdleMs = redisProps.streams.fetcher.autoclaimMinIdleMs
            )
        }
    }

    override fun onInit() {
        for (fetcher in fetchers) {
            fetcher.onInit()
        }
    }

    override fun close() {
        for (fetcher in fetchers) {
            fetcher.close()
        }
    }


}
