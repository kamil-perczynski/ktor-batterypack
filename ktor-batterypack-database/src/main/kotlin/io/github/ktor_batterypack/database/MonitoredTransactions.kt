package io.github.ktor_batterypack.database

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

class MonitoredTransactions(
    private val database: Database,
    private val meterRegistry: MeterRegistry
) {

    suspend fun <T> suspendTx(methodId: String, block: suspend () -> T): T {
        val sample = Timer.start(meterRegistry)

        try {
            val result = suspendTransaction(database) {
                block()
            }
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
