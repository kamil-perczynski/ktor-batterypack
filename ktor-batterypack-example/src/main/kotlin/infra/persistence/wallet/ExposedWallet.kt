package io.github.kperczynski.infra.persistence.wallet

import org.jetbrains.exposed.v1.core.dao.id.UIntIdTable

object ExposedWallet : UIntIdTable(name = "wallets") {
    val userId = uinteger("user_id")
    val balance = decimal("balance", precision = 16, scale = 2)
}
