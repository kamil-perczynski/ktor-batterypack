package io.github.ktor_batterypack.redis

import io.github.ktor_batterypack.core.health.ReadinessCheck
import io.github.ktor_batterypack.redis.monitoring.RedisReadinessCheck
import io.lettuce.core.ClientOptions
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.SocketOptions
import io.lettuce.core.TimeoutOptions
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.metrics.MicrometerCommandLatencyRecorder
import io.lettuce.core.metrics.MicrometerOptions
import io.lettuce.core.resource.ClientResources
import io.micrometer.core.instrument.MeterRegistry
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Singleton
import java.time.Duration

internal fun RedisProps.toRedisUri(): RedisURI {
    val uri = RedisURI.create(url)

    when {
        username != null -> uri.setAuthentication(username, password.orEmpty())
        password != null -> uri.setAuthentication(password)
    }
    ssl?.let(uri::setSsl)
    database?.let(uri::setDatabase)
    clientName?.let(uri::setClientName)
    when {
        timeoutMs != null -> uri.setTimeout(Duration.ofMillis(timeoutMs))
        uri.timeout == RedisURI.DEFAULT_TIMEOUT_DURATION -> uri.setTimeout(DEFAULT_COMMAND_TIMEOUT)
    }

    return uri
}

private val DEFAULT_COMMAND_TIMEOUT: Duration = Duration.ofSeconds(10)

internal fun redisClientOptions(redisProps: RedisProps, commandTimeout: Duration): ClientOptions {
    val keepAlive = SocketOptions.KeepAliveOptions.builder()
        .enable()
        .idle(Duration.ofSeconds(30))
        .interval(Duration.ofSeconds(10))
        .count(3)
        .build()

    val socketOptions = SocketOptions.builder()
        .tcpNoDelay(true)
        .keepAlive(keepAlive)
        .build()

    val timeoutSource = RedisCommandTimeoutSource(
        commandTimeout = commandTimeout,
        blockingTimeout = Duration.ofMillis(redisProps.streams.fetcher.fetchingTimeout)
    )

    return ClientOptions.builder()
        .autoReconnect(true)
        .pingBeforeActivateConnection(true)
        .socketOptions(socketOptions)
        .timeoutOptions(TimeoutOptions.builder().timeoutSource(timeoutSource).build())
        .build()
}

@Module
@ComponentScan("io.github.ktor_batterypack.redis")
class KtorBatterypackRedisModule {

    @Singleton(binds = [ReadinessCheck::class])
    fun redisCheck(redisClient: RedisClient): RedisReadinessCheck {
        return RedisReadinessCheck(redisClient)
    }

    @Singleton(binds = [AutoCloseable::class])
    fun redisClient(@Provided redisProps: RedisProps, meterRegistry: MeterRegistry): RedisClient {
        val options = MicrometerOptions.builder()
            .localDistinction(false)
            .build()

        val resources = ClientResources.builder()
            .commandLatencyRecorder(MicrometerCommandLatencyRecorder(meterRegistry, options))
            .build()

        val redisUri = redisProps.toRedisUri()
        val redisClient = RedisClient.create(resources, redisUri)
        redisClient.options = redisClientOptions(redisProps, redisUri.timeout)
        return redisClient
    }

    @Singleton
    fun statefulRedisConnection(redisClient: RedisClient): StatefulRedisConnection<String, String> {
        return redisClient.connect()
    }

}
