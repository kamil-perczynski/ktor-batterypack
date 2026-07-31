package io.github.kperczynski.domain.wallet

import java.math.BigDecimal

data class Wallet(
    val id: UInt,
    val userId: UInt,
    val balance: BigDecimal
)
