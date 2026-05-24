package io.github.kperczynski.libs.redis

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class RedisStreamMetrics(private val meterRegistry: MeterRegistry) {

    private val lagValues = ConcurrentHashMap<Pair<String, String>, AtomicLong>()
    private val pendingValues = ConcurrentHashMap<Pair<String, String>, AtomicLong>()

    fun recordListenerDuration(stream: String, consumerGroup: String, outcome: String, durationNanos: Long) {
        Timer.builder("redis.stream.listener.duration")
            .tag("stream", stream)
            .tag("consumerGroup", consumerGroup)
            .tag("outcome", outcome)
            .register(meterRegistry)
            .record(durationNanos, TimeUnit.NANOSECONDS)
    }

    fun recordAutoclaimReclaimed(stream: String, count: Int) {
        Counter.builder("redis.stream.autoclaim.reclaimed")
            .tag("stream", stream)
            .register(meterRegistry)
            .increment(count.toDouble())
    }

    fun recordLag(stream: String, consumerGroup: String, lagMs: Long, pendingCount: Long) {
        val key = stream to consumerGroup

        lagValues.computeIfAbsent(key) { _ ->
            val value = AtomicLong(lagMs)
            Gauge.builder("redis.stream.lag") { value.get().toDouble() }
                .tag("stream", stream)
                .tag("consumerGroup", consumerGroup)
                .register(meterRegistry)
            value
        }.set(lagMs)

        pendingValues.computeIfAbsent(key) { _ ->
            val value = AtomicLong(pendingCount)
            Gauge.builder("redis.stream.lag.pending") { value.get().toDouble() }
                .tag("stream", stream)
                .tag("consumerGroup", consumerGroup)
                .register(meterRegistry)
            value
        }.set(pendingCount)
    }
}
