package io.github.kperczynski.infra.persistence.wallet

import io.github.kperczynski.domain.wallet.Wallet
import io.github.kperczynski.domain.wallet.WalletRepo
import io.github.ktor_batterypack.core.exception.ResourceMissingException
import io.micrometer.core.annotation.Timed
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.koin.core.annotation.Singleton
import java.math.BigDecimal

@Singleton
@Timed
class ExposedWalletRepo(private val database: Database) : WalletRepo {

    override suspend fun create(wallet: Wallet): Wallet {
        return suspendTransaction(database) {
            val entity = WalletEntity.new {
                userId = wallet.userId
                balance = wallet.balance
            }
            toWallet(entity)
        }
    }

    override suspend fun find(id: UInt): Wallet? {
        return suspendTransaction(database) {
            WalletEntity.findById(id)?.let { toWallet(it) }
        }
    }

    override suspend fun topup(id: UInt, amount: BigDecimal): Wallet {
        return suspendTransaction(database) {
            val entity = WalletEntity.findById(id)
                ?: throw ResourceMissingException(Wallet::class.java, id)
            entity.balance = entity.balance.add(amount)
            toWallet(entity)
        }
    }

    private fun toWallet(entity: WalletEntity): Wallet {
        return Wallet(
            id = entity.id.value,
            userId = entity.userId,
            balance = entity.balance
        )
    }
}
