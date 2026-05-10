package io.github.kperczynski.di

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.kperczynski.config.AppConfig
import io.github.kperczynski.config.DatabaseConfig
import io.github.kperczynski.config.loadConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val log = LoggerFactory.getLogger(KtorFrameModule::class.java)

@KoinApplication
object KtorFrameApp

@Module(createdAtStart = true)
@ComponentScan("io.github.kperczynski")
@Configuration
class KtorFrameModule {

    @Singleton(createdAtStart = true)
    fun appConfig(): AppConfig {
        return loadConfig()
    }

    @Singleton
    fun databaseConfig(appConfig: AppConfig) = appConfig.database

    @Singleton(createdAtStart = true, binds = [DataSource::class])
    fun dataSource(config: DatabaseConfig): HikariDataSource {
        log.info("Connected to database at ${config.url} with pool size ${config.poolSize}")

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.url
            username = config.user
            password = config.password
            driverClassName = config.driver
            maximumPoolSize = config.poolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        return HikariDataSource(hikariConfig)
    }

    @Singleton(createdAtStart = true)
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