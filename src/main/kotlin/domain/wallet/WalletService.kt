package io.github.kperczynski.domain.wallet

import io.github.ktor_batterypack.core.exception.ErrorCodeException
import io.github.ktor_batterypack.core.exception.ResourceMissingException
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory
import java.math.BigDecimal

private val log = LoggerFactory.getLogger(WalletService::class.java)

@Singleton
class WalletService(
    private val walletRepo: WalletRepo,
    private val walletEventPublisher: WalletEventPublisher
) {

    suspend fun requestTopup(amount: BigDecimal, walletId: UInt?, userId: UInt?): Wallet {
        log.info("Requesting topup with amount {}, walletId {}, userId {}", amount, walletId, userId)

        val wallet = resolveWallet(walletId, userId)

        walletEventPublisher.publish(
            WalletEvent(
                walletId = wallet.id.toString(),
                amount = amount.toPlainString(),
                userId = wallet.userId.toString(),
                type = WalletEventType.WALLET_TOPUP_REQUESTED
            )
        )

        return wallet
    }

    private suspend fun resolveWallet(walletId: UInt?, userId: UInt?): Wallet {
        if (walletId != null) {
            val existing = walletRepo.find(walletId)
            if (existing != null) {
                return existing
            }
            if (userId != null) {
                log.info("Wallet {} not found, creating with zero balance for user {}", walletId, userId)
                return walletRepo.create(
                    Wallet(
                        id = walletId,
                        userId = userId,
                        balance = BigDecimal.ZERO
                    )
                )
            }
            throw ErrorCodeException(WalletErrorCode.WALLET_NOT_FOUND_WITHOUT_USER)
        }

        if (userId != null) {
            return walletRepo.create(
                Wallet(
                    id = 0u,
                    userId = userId,
                    balance = BigDecimal.ZERO
                )
            )
        }

        throw ErrorCodeException(WalletErrorCode.MISSING_WALLET_OR_USER)
    }

    suspend fun find(walletId: UInt): Wallet {
        log.info("Finding wallet with id: {}", walletId)
        return walletRepo.find(walletId)
            ?: throw ResourceMissingException(Wallet::class.java, walletId)
    }

    suspend fun processTopup(walletId: UInt, amount: BigDecimal, userId: UInt): Wallet {
        log.info("Processing topup for wallet {} with amount {} for user {}", walletId, amount, userId)
        val updatedWallet = walletRepo.topup(walletId, amount)

        walletEventPublisher.publish(
            WalletEvent(
                walletId = updatedWallet.id.toString(),
                amount = updatedWallet.balance.toPlainString(),
                userId = updatedWallet.userId.toString(),
                type = WalletEventType.WALLET_BALANCE_CHANGED
            )
        )

        return updatedWallet
    }

}
