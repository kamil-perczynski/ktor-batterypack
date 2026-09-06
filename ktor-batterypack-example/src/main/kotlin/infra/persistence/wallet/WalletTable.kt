package io.github.kperczynski.infra.persistence.wallet

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UIntIdTable
import org.jetbrains.exposed.v1.dao.UIntEntity
import org.jetbrains.exposed.v1.dao.UIntEntityClass

object WalletTable : UIntIdTable(name = "wallets") {
    val userId = uinteger("user_id")
    val balance = decimal("balance", precision = 16, scale = 2)
}

class WalletEntity(id: EntityID<UInt>) : UIntEntity(id) {
    companion object : UIntEntityClass<WalletEntity>(WalletTable)

    var userId by WalletTable.userId
    var balance by WalletTable.balance
}
