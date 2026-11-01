package io.github.ktor_batterypack.redis_stream.bgloops

import io.github.ktor_batterypack.redis_stream.monitoring.RedisStreamMetrics
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisCommandExecutionException
import io.lettuce.core.RedisException
import io.lettuce.core.RedisLoadingException
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.IOException

private val log = LoggerFactory.getLogger(ReconnectingRedisConnection::class.java)

class ReconnectingRedisConnection(
    private val redisClient: RedisClient,
    private val name: String,
    private val metrics: RedisStreamMetrics,
    private val backoff: ExponentialBackoff = ExponentialBackoff(),
) : AutoCloseable {

    @Volatile
    private var connection: StatefulRedisConnection<String, String>? = null

    private var failedAttempts = 0

    suspend fun <T> execute(block: suspend (StatefulRedisConnection<String, String>) -> T): T {
        while (true) {
            try {
                val result = block(currentConnection())
                onSuccess()
                return result
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!isTransportFailure(e)) {
                    throw e
                }
                onTransportFailure(e)
            }
        }
    }

    private suspend fun currentConnection(): StatefulRedisConnection<String, String> {
        val current = connection
        if (current != null) {
            return current
        }

        val created = withContext(Dispatchers.IO) {
            redisClient.connect()
        }
        connection = created
        return created
    }

    private fun onSuccess() {
        if (failedAttempts > 0) {
            log.info("Redis connection for {} recovered after {} failed attempt(s)", name, failedAttempts)
            failedAttempts = 0
            backoff.reset()
        }
    }

    private suspend fun onTransportFailure(e: Exception) {
        failedAttempts++
        metrics.recordConnectionFailure(name, e::class.java.simpleName)
        dropConnection()

        val retryIn = backoff.nextDelay()
        log.warn(
            "Redis connection for {} failed (attempt {}), reconnecting in {}: {}",
            name,
            failedAttempts,
            retryIn,
            e.message
        )
        delay(retryIn)
    }

    private fun dropConnection() {
        val current = connection ?: return
        connection = null

        try {
            current.closeAsync()
        } catch (e: Exception) {
            log.debug("Failed to close broken Redis connection for {}: {}", name, e.message)
        }
    }

    private fun isTransportFailure(e: Exception): Boolean {
        return when (e) {
            is RedisLoadingException -> true
            is RedisCommandExecutionException -> false
            is RedisException, is IOException -> true
            else -> false
        }
    }

    override fun close() {
        val current = connection ?: return
        connection = null

        if (current.isOpen) {
            log.debug("Closing Redis connection for {}", name)
            current.close()
        }
    }

}
