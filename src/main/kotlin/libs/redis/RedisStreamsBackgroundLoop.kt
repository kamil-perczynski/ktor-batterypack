package io.github.kperczynski.libs.redis

/**
 * Contract for a background loop that runs continuously to interact with Redis Streams.
 *
 * Implementations (e.g., fetching, autoclaim, lag monitoring) are managed by
 * [RedisStreamFetcher], which orchestrates their lifecycle in the following order:
 *
 * 1. [init] – called once to establish connections and prepare resources.
 * 2. [start] – called once to begin the background coroutine(s).
 * 3. [close] – called once on shutdown to cancel coroutines and release resources.
 *
 * **Important:** The three methods must be invoked exactly once each, in the order above.
 * Calling [start] before [init] or calling [init] twice will result in undefined behavior.
 */
interface RedisStreamsBackgroundLoop {

    /**
     * Prepares the loop for execution.
     *
     * Typical responsibilities include opening Redis connections, creating coroutine scopes,
     * and any other one-time setup that must happen before [start] is called.
     */
    fun init()

    /**
     * Starts the background processing.
     *
     * Implementations should launch their coroutine(s) here. This method is called **after**
     * [init] and must not be called more than once per instance.
     *
     * @param fetcherId    Unique identifier of the consumer (used as the Redis consumer name).
     * @param listeners    Map of stream name → listener for dispatching messages.
     * @param consumerGroup The Redis Streams consumer group all operations belong to.
     */
    fun start(
        fetcherId: String,
        listeners: Map<String, RedisStreamListener>,
        consumerGroup: String,
    )

    /**
     * Stops the loop and releases all held resources.
     *
     * Implementations must cancel any active coroutines, wait for graceful termination,
     * and close open connections. This method is idempotent and safe to call multiple times.
     */
    fun close()
}