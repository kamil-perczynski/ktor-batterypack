package io.github.ktor_batterypack.redis_stream

/**
 * Handle returned by [RedisStreamsBackgroundLoop.start].
 *
 * Closing the handle cancels the background coroutine(s) and releases
 * the per-fetcher connection.
 */
interface LoopHandle : AutoCloseable
