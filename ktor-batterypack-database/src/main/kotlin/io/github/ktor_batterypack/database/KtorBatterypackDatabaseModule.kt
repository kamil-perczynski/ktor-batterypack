package io.github.ktor_batterypack.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.ktor_batterypack.core.health.ReadinessCheck
import io.micrometer.core.instrument.MeterRegistry
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val log = LoggerFactory.getLogger(KtorBatterypackDatabaseModule::class.java)

@Module
@ComponentScan("io.github.ktor_batterypack.database")
class KtorBatterypackDatabaseModule {

    @Singleton(binds = [DataSource::class])
    fun dataSource(@Provided props: DatabaseProps, @Provided meterRegistry: MeterRegistry): HikariDataSource {
        log.info("Connected to database at {} with pool size: {}", props.url, props.poolSize)

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = props.url
            username = props.user
            password = props.password
            driverClassName = props.driver
            maximumPoolSize = props.poolSize
            isAutoCommit = true
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            metricRegistry = meterRegistry
            validate()
        }

        return HikariDataSource(hikariConfig)
    }

    @Singleton
    fun hikariCloser(dataSource: HikariDataSource): AutoCloseable {
        log.info("Registering AutoCloseable for DataSource: $dataSource")

        return AutoCloseable {
            log.info("Closing DataSource: {}", dataSource)
            dataSource.close()
            log.info("DataSource closed successfully.")
        }
    }

    @Singleton(binds = [ReadinessCheck::class])
    fun databaseCheck(dataSource: DataSource): DatabaseReadinessCheck {
        return DatabaseReadinessCheck(dataSource)
    }

}
