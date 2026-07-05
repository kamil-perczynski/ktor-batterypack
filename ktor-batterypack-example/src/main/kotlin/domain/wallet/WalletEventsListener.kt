package io.github.kperczynski.domain.wallet

import io.github.ktor_batterypack.redis.RedisStreamListener
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory
import tools.jackson.databind.json.JsonMapper
import java.math.BigDecimal

private val log = LoggerFactory.getLogger(WalletEventsListener::class.java)

@Singleton
class WalletEventsListener(
    private val jsonMapper: JsonMapper,
    private val walletService: WalletService
) : RedisStreamListener {

    override fun stream(): String = WALLET_EVENTS_TOPIC

    override suspend fun onMessage(payload: String, headers: Map<String, String>) {
        val event = jsonMapper.readValue(payload, WalletEvent::class.java)
        log.info("Received wallet event: {}, headers: {}", event, headers)

        if (event.type == WalletEventType.WALLET_TOPUP_REQUESTED) {
            val walletId = event.walletId.toUInt()
            val amount = BigDecimal(event.amount)
            val userId = event.userId.toUInt()

            walletService.processTopup(walletId, amount, userId)
        }

        if (event.type == WalletEventType.WALLET_BALANCE_CHANGED) {
            log.info("Wallet {} balance changed to {} for user {}", event.walletId, event.amount, event.userId)
        }
    }

}
