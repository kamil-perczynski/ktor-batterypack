package io.github.kperczynski.libs.redis.monitoring

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.Timer
import java.util.concurrent.TimeUnit

data class StreamConsumerMetrics(
    val stream: String,
    val consumerGroup: String,
    val consumerId: String,
    val pending: Long,
    val idle: Long,
    val inactive: Long?
)

class RedisStreamMetrics(private val meterRegistry: MeterRegistry) {

    private val streamInfoGauge = MultiGauge.builder("redis.stream.info")
        .description("Redis stream consumer information")
        .register(meterRegistry)

    fun recordListenerDuration(
        stream: String,
        consumerGroup: String,
        throwableClass: String,
        durationNanos: Long
    ) {
        Timer.builder("redis.stream.listener.duration")
            .tag("stream", stream)
            .tag("consumerGroup", consumerGroup)
            .tag("throwable", throwableClass)
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry)
            .record(durationNanos, TimeUnit.NANOSECONDS)
    }

    fun recordAutoclaimReclaimed(stream: String, count: Int) {
        Counter.builder("redis.stream.autoclaim.reclaimed")
            .tag("stream", stream)
            .register(meterRegistry)
            .increment(count.toDouble())
    }

    fun recordStreamInfo(consumers: List<StreamConsumerMetrics>) {
        streamInfoGauge.register(
            consumers.flatMap { c ->
                listOf<MultiGauge.Row<*>>(
                    MultiGauge.Row.of(
                        Tags.of(
                            "stream", c.stream,
                            "consumerGroup", c.consumerGroup,
                            "consumer", c.consumerId,
                            "type", "idle",
                        ),
                        c.idle.toDouble()
                    ),

                    MultiGauge.Row.of(
                        Tags.of(
                            "stream", c.stream,
                            "consumerGroup", c.consumerGroup,
                            "consumer", c.consumerId,
                            "type", "pending",
                        ),
                        c.pending.toDouble()
                    ),

                    MultiGauge.Row.of(
                        Tags.of(
                            "stream", c.stream,
                            "consumerGroup", c.consumerGroup,
                            "consumer", c.consumerId,
                            "type", "inactive",
                        ),
                        c.inactive?.toDouble() ?: -1
                    )
                )
            }
        )
    }
}
