package io.github.kperczynski.infra.monitoring

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.annotation.Singleton
import java.util.function.Supplier

@Singleton
class MonitoredTransactions(
    private val database: Database,
    private val meterRegistry: MeterRegistry
) {

    fun <T> tx(methodId: String, block: () -> T): T {
        val sample = Timer.start(meterRegistry)

        val supplier = Supplier<T> {
            transaction(database) {
                block()
            }
        }

        try {
            val result = supplier.get()
            sample.stop(
                meterRegistry.timer(
                    "repo.operation",
                    "methodId",
                    methodId,
                    "throwable",
                    "n/a"
                )
            )
            return result

        } catch (ex: Throwable) {
            sample.stop(
                meterRegistry.timer(
                    "repo.operation",
                    "methodId",
                    methodId,
                    "throwable",
                    ex::class.java.simpleName
                )
            )
            throw ex
        }
    }
}