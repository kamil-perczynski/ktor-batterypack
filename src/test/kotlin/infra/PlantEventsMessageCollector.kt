package io.github.kperczynski.infra

import io.github.kperczynski.domain.plant.PLANT_EVENTS_TOPIC
import io.github.kperczynski.libs.redis.RedisStreamListener
import io.github.kperczynski.libs.redis.RedisStreamListenerGroups.Companion.TEST_GROUP
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class PlantEventsMessageCollector : RedisStreamListener {

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

    override fun group(): String = TEST_GROUP

    override fun stream(): String = PLANT_EVENTS_TOPIC
}

data class CapturedMsg(val payload: String, val headers: Map<String, String>)