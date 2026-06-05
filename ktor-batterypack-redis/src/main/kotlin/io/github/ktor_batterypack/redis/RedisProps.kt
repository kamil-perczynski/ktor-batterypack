package io.github.ktor_batterypack.redis

data class RedisProps(
    val url: String = "redis://localhost:6379",
    val fetcher: FetcherProps = FetcherProps(),
    val publisher: PublisherProps = PublisherProps()
)

data class PublisherProps(
    val retentionMs: Long = 7_200_000L
)

data class FetcherProps(
    val consumerPrefix: String = "Main-",
    val consumerGroup: String = "florin",
    val fetchingTimeout: Long = 5000L,
    val fetchingCount: Long = 100L,
    val autoclaimIntervalMs: Long = 30000L,
    val autoclaimMinIdleMs: Long = 60000L,
    val autoclaimCount: Long = 10L,
    val lagCheckIntervalMs: Long = 30000L,
)
