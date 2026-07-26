package io.github.ktor_batterypack.redis

import com.fasterxml.jackson.annotation.JsonPropertyDescription

/**
 * Redis connection and stream configuration properties.
 */
data class RedisProps(
    @param:JsonPropertyDescription("Redis connection URL")
    val url: String = "redis://localhost:6379",
    @param:JsonPropertyDescription("Stream fetcher consumer configuration")
    val fetcher: FetcherProps = FetcherProps(),
    @param:JsonPropertyDescription("Stream publisher configuration")
    val publisher: PublisherProps = PublisherProps()
)

/**
 * Configuration for publishing messages to Redis streams.
 */
data class PublisherProps(
    @param:JsonPropertyDescription("How long published messages are retained in the stream, in milliseconds")
    val retentionMs: Long = 7_200_000L
)

/**
 * Configuration for consuming messages from Redis streams.
 */
data class FetcherProps(
    @param:JsonPropertyDescription("Prefix used when creating consumer names")
    val consumerPrefix: String = "Main-",
    @param:JsonPropertyDescription("Name of the Redis consumer group")
    val consumerGroup: String = "florin",
    @param:JsonPropertyDescription("Maximum time to wait for new messages when fetching, in milliseconds")
    val fetchingTimeout: Long = 5000L,
    @param:JsonPropertyDescription("Maximum number of messages to fetch in a single batch")
    val fetchingCount: Long = 100L,
    @param:JsonPropertyDescription("Interval between autoclaim attempts, in milliseconds")
    val autoclaimIntervalMs: Long = 30000L,
    @param:JsonPropertyDescription("Minimum idle time before a message is eligible for autoclaim, in milliseconds")
    val autoclaimMinIdleMs: Long = 60000L,
    @param:JsonPropertyDescription("Maximum number of messages to claim in a single autoclaim pass")
    val autoclaimCount: Long = 10L,
    @param:JsonPropertyDescription("Interval between lag checks, in milliseconds")
    val lagCheckIntervalMs: Long = 30000L,
)
