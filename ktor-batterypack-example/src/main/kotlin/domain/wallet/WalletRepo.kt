package io.github.kperczynski.domain.wallet

import java.math.BigDecimal

interface WalletRepo {

    suspend fun create(wallet: Wallet): Wallet

    suspend fun find(id: UInt): Wallet?

    suspend fun topup(id: UInt, amount: BigDecimal): Wallet

}
