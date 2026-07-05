package io.github.kperczynski.infra.persistence.wallet

import io.github.kperczynski.domain.wallet.Wallet
import io.github.kperczynski.domain.wallet.WalletRepo
import io.github.ktor_batterypack.core.di.InitCallback
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory
import java.math.BigDecimal

private val log = LoggerFactory.getLogger(ExposedWalletRepo::class.java)

@Singleton
class ExposedWalletRepo(private val database: Database) : WalletRepo, InitCallback {

    override fun onInit() {
        transaction(database) {
            log.info("Creating 'wallets' table if it does not exist...")
            SchemaUtils.create(ExposedWallet)
        }
    }

    override suspend fun create(wallet: Wallet): Wallet {
        return suspendTransaction(database) {
            val newRecord = ExposedWallet.insert {
                it[userId] = wallet.userId
                it[balance] = wallet.balance
            }
            val id = newRecord[ExposedWallet.id].value

            wallet.copy(id = id)
        }
    }

    override suspend fun find(id: UInt): Wallet? {
        return suspendTransaction(database) {
            ExposedWallet.selectAll()
                .where { ExposedWallet.id eq id }
                .map { toWallet(it) }
                .singleOrNull()
        }
    }

    override suspend fun topup(id: UInt, amount: BigDecimal): Wallet {
        return suspendTransaction(database) {
            val currentBalance = ExposedWallet.selectAll()
                .where { ExposedWallet.id eq id }
                .map { it[ExposedWallet.balance] }
                .single()

            val newBalance = currentBalance.add(amount)

            ExposedWallet.update({ ExposedWallet.id eq id }) {
                it[balance] = newBalance
            }

            ExposedWallet.selectAll()
                .where { ExposedWallet.id eq id }
                .map { toWallet(it) }
                .single()
        }
    }

}

private fun toWallet(row: ResultRow): Wallet {
    return Wallet(
        id = row[ExposedWallet.id].value,
        userId = row[ExposedWallet.userId],
        balance = row[ExposedWallet.balance]
    )
}
