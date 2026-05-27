package io.github.kperczynski.libs.redis

/**
 * Contract for a background loop that runs continuously to interact with Redis Streams.
 *
 * Implementations (e.g., fetching, autoclaim, lag monitoring) are managed by
 * [RedisStreamFetcher], which starts them via [start] and closes the returned handle
 * on shutdown.
 *
 * Each call to [start] creates an independent runtime (connection + coroutine job),
 * allowing a single singleton implementation to serve multiple fetchers.
 *
 * The returned [AutoCloseable] must cancel coroutines and release resources when closed.
 */
interface RedisStreamsBackgroundLoop {

    /**
     * Starts the background processing.
     *
     * Implementations should open a fresh Redis connection and launch coroutine(s) here.
     *
     * @param fetcherId    Unique identifier of the consumer (used as the Redis consumer name).
     * @param listeners    Map of stream name → listener for dispatching messages.
     * @param consumerGroup The Redis Streams consumer group all operations belong to.
     * @return A [LoopHandle] that cancels the loop and releases its connection when closed.
     */
    fun start(
        fetcherId: String,
        listeners: Map<String, RedisStreamListener>,
        consumerGroup: String,
    ): LoopHandle
}
