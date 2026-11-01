package io.github.ktor_batterypack.redis_stream.bgloops

import kotlin.math.min
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ExponentialBackoff(
    private val initialDelay: Duration = 100.milliseconds,
    private val maxDelay: Duration = 10.seconds,
) {

    private var attempt = 0

    fun nextDelay(): Duration {
        val delay = initialDelay * 2.0.pow(min(attempt, 30))
        attempt++
        return minOf(delay, maxDelay)
    }

    fun reset() {
        attempt = 0
    }

}
