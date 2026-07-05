package io.github.kperczynski.domain.wallet

data class WalletEvent(
    val walletId: String,
    val amount: String,
    val userId: String,
    val type: WalletEventType
)

enum class WalletEventType {
    WALLET_TOPUP_REQUESTED,
    WALLET_BALANCE_CHANGED
}
