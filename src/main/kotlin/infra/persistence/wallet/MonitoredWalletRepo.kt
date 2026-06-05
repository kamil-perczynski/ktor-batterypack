package io.github.kperczynski.infra.persistence.wallet

import io.github.kperczynski.domain.wallet.Wallet
import io.github.kperczynski.domain.wallet.WalletRepo
import io.github.ktor_batterypack.database.MonitoredTransactions
import org.koin.core.annotation.Singleton
import java.math.BigDecimal

@Singleton
class MonitoredWalletRepo(
    private val delegate: ExposedWalletRepo,
    private val monitoredTransactions: MonitoredTransactions
) : WalletRepo {

    override suspend fun create(wallet: Wallet): Wallet {
        return monitoredTransactions.suspendTx("WalletRepo.create") {
            delegate.create(wallet)
        }
    }

    override suspend fun find(id: UInt): Wallet? {
        return monitoredTransactions.suspendTx("WalletRepo.find") {
            delegate.find(id)
        }
    }

    override suspend fun topup(id: UInt, amount: BigDecimal): Wallet {
        return monitoredTransactions.suspendTx("WalletRepo.topup") {
            delegate.topup(id, amount)
        }
    }

}
