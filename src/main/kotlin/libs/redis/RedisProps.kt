package io.github.kperczynski.libs.redis

data class RedisProps(
    val url: String = "redis://localhost:6379",
    val fetcher: FetcherProps = FetcherProps()
)

data class FetcherProps(
    val consumerPrefix: String = "Main-",
    val consumerGroup: String = "florin",
    val fetchingTimeout: Long = 5000L,
    val fetchingCount: Long = 10L,
    val autoclaimIntervalMs: Long = 30000L,
    val autoclaimMinIdleMs: Long = 60000L,
    val autoclaimCount: Long = 10L,
)