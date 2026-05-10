package io.github.kperczynski.infra.persistence.user

import io.github.kperczynski.domain.user.User
import io.github.kperczynski.domain.user.UserRepo
import io.github.kperczynski.infra.monitoring.MonitoredTransactions
import org.koin.core.annotation.Singleton

@Singleton(binds = [UserRepo::class])
class MonitoredUserRepo(
    private val delegate: ExposedUserRepo,
    private val monitoredTransactions: MonitoredTransactions
) : UserRepo {

    override fun create(user: User): User {
        return monitoredTransactions.tx("UserRepo.create") {
            delegate.create(user)
        }
    }

    override fun find(id: UInt): User {
        return monitoredTransactions.tx("UserRepo.find") {
            delegate.find(id)
        }
    }

    override fun update(user: User) {
        monitoredTransactions.tx("UserRepo.update") {
            delegate.update(user)
        }
    }

    override fun delete(id: UInt) {
        monitoredTransactions.tx("UserRepo.delete") {
            delegate.delete(id)
        }
    }
}
