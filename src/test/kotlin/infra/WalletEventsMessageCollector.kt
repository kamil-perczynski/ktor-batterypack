package io.github.kperczynski.infra

import io.github.kperczynski.domain.wallet.WALLET_EVENTS_TOPIC
import io.github.kperczynski.domain.wallet.WalletEvent
import io.github.kperczynski.libs.redis.RedisStreamListener
import io.github.kperczynski.libs.redis.RedisStreamListenerGroups.Companion.TEST_GROUP
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.Singleton
import tools.jackson.databind.json.JsonMapper
import kotlin.time.Duration.Companion.seconds

@Singleton
class WalletEventsMessageCollector(
    private val jsonMapper: JsonMapper
) : RedisStreamListener {

    private val channel = Channel<WalletEvent>(capacity = 10)
    private var expectedCount = 0

    fun expectResult(count: Int = 1) {
        while (channel.tryReceive().isSuccess) {
            /* drain stale messages */
        }
        expectedCount = count
    }

    override suspend fun onMessage(payload: String, headers: Map<String, String>) {
        channel.send(jsonMapper.readValue(payload, WalletEvent::class.java))
    }

    suspend fun awaitMessages(): List<WalletEvent> {
        val results = mutableListOf<WalletEvent>()
        repeat(expectedCount) {
            val msg = withTimeoutOrNull(3.seconds) { channel.receive() }
                ?: return results
            results.add(msg)
        }
        return results
    }

    override fun group(): String = TEST_GROUP

    override fun stream(): String = WALLET_EVENTS_TOPIC

}
