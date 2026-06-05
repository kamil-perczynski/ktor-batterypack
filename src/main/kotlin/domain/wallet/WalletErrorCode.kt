package io.github.kperczynski.domain.wallet

import io.github.ktor_batterypack.core.exception.ErrorCode

enum class WalletErrorCode(override val message: String) : ErrorCode {
    MISSING_WALLET_OR_USER("Either walletId or userId must be provided"),
    WALLET_NOT_FOUND_WITHOUT_USER("Wallet not found and cannot be created without userId"),;

    override val code: String
        get() = name
}
