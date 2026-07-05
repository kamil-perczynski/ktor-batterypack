package io.github.kperczynski.domain.wallet

import io.github.ktor_batterypack.redis.RedisStreamPublisher
import org.koin.core.annotation.Singleton

const val WALLET_EVENTS_TOPIC = "wallet_events"

@Singleton
class WalletEventPublisher(private val redisStreamPublisher: RedisStreamPublisher) {

    fun publish(event: WalletEvent) {
        redisStreamPublisher.publish(
            stream = WALLET_EVENTS_TOPIC,
            payload = event,
            headers = mapOf("X-Correlation-Id" to event.walletId),
        )
    }
}
