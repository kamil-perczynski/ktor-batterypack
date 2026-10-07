package io.github.kperczynski.infra

import io.github.kperczynski.domain.user.USER_EVENTS_TOPIC
import io.github.ktor_batterypack.redis_stream.RedisStreamListener
import io.github.ktor_batterypack.redis_stream.RedisStreamListenerConfig
import io.github.ktor_batterypack.redis.testing.TEST_CONSUMER_GROUP
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class UserEventsMessageCollector : RedisStreamListener {

    private var result: CompletableDeferred<CapturedMsg> = CompletableDeferred()

    fun expectResult() {
        result = CompletableDeferred()
    }

    override suspend fun onMessage(payload: String, headers: Map<String, String>) {
        if (!this.result.isCompleted) {
            result.complete(CapturedMsg(payload, headers))
        }
    }

    suspend fun lastMessage(): CapturedMsg? {
        return withTimeoutOrNull(3.seconds) {
            result.await()
        }
    }

    override fun config(): RedisStreamListenerConfig = RedisStreamListenerConfig(
        streamId = USER_EVENTS_TOPIC,
        consumerGroup = TEST_CONSUMER_GROUP
    )
}
