package io.github.ktor_batterypack.redis

/**
 * Handle returned by [RedisStreamsBackgroundLoop.start].
 *
 * Closing the handle cancels the background coroutine(s) and releases
 * the per-fetcher connection.
 */
interface LoopHandle : AutoCloseable
