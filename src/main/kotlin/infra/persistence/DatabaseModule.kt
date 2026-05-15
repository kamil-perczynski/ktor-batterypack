package io.github.kperczynski.infra.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.kperczynski.libs.db.DatabaseProps
import io.micrometer.core.instrument.MeterRegistry
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val log = LoggerFactory.getLogger(DatabaseModule::class.java)

@Module
@Configuration
class DatabaseModule {

    @Singleton(binds = [DataSource::class])
    fun dataSource(props: DatabaseProps, @Provided meterRegistry: MeterRegistry): HikariDataSource {
        log.info("Connected to database at {} with pool size: {}", props.url, props.poolSize)

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = props.url
            username = props.user
            password = props.password
            driverClassName = props.driver
            maximumPoolSize = props.poolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            metricRegistry = meterRegistry
            validate()
        }

        return HikariDataSource(hikariConfig)
    }

    @Singleton
    fun database(dataSource: DataSource): Database {
        return Database.connect(dataSource)
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

}