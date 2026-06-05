package io.github.kperczynski.infra.persistence.user

import io.github.kperczynski.domain.user.User
import io.github.kperczynski.domain.user.UserRepo
import io.github.ktor_batterypack.database.MonitoredTransactions
import org.koin.core.annotation.Singleton

@Singleton(binds = [UserRepo::class])
class MonitoredUserRepo(
    private val delegate: ExposedUserRepo,
    private val monitoredTransactions: MonitoredTransactions
) : UserRepo {

    override suspend fun create(user: User): User {
        return monitoredTransactions.suspendTx("UserRepo.create") {
            delegate.create(user)
        }
    }

    override suspend fun find(id: UInt): User {
        return monitoredTransactions.suspendTx("UserRepo.find") {
            delegate.find(id)
        }
    }

    override suspend fun update(user: User) {
        monitoredTransactions.suspendTx("UserRepo.update") {
            delegate.update(user)
        }
    }

    override suspend fun delete(id: UInt) {
        monitoredTransactions.suspendTx("UserRepo.delete") {
            delegate.delete(id)
        }
    }
}
