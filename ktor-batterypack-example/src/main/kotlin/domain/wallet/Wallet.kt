package io.github.kperczynski.domain.wallet

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class Wallet(
    val id: UInt,
    val userId: UInt,
    @Contextual
    val balance: BigDecimal
)
