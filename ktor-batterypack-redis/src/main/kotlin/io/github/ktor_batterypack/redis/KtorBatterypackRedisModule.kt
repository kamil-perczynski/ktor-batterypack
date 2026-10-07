package io.github.ktor_batterypack.redis

import io.github.ktor_batterypack.core.health.ReadinessCheck
import io.github.ktor_batterypack.redis.monitoring.RedisReadinessCheck
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
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
    timeoutMs?.let { uri.setTimeout(Duration.ofMillis(it)) }

    return uri
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

        return RedisClient.create(resources, redisProps.toRedisUri())
    }

    @Singleton
    fun statefulRedisConnection(redisClient: RedisClient): StatefulRedisConnection<String, String> {
        return redisClient.connect()
    }

}
