package io.github.kperczynski.infra

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.kperczynski.libs.db.DatabaseProps
import io.github.kperczynski.libs.di.BannerPrinter
import io.github.kperczynski.libs.di.KoinLifecycleListener
import io.github.kperczynski.libs.di.LifecycleListener
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
    fun appConfig(): AppProps {
        return loadConfig()
    }

    @Singleton
    fun databaseProps(appProps: AppProps) = appProps.database

    @Singleton(createdAtStart = true, binds = [DataSource::class])
    fun dataSource(props: DatabaseProps): HikariDataSource {
        log.info("Connected to database at ${props.url} with pool size ${props.poolSize}")

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = props.url
            username = props.user
            password = props.password
            driverClassName = props.driver
            maximumPoolSize = props.poolSize
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

    @Singleton
    fun bannerPrinter(appProps: AppProps) = BannerPrinter(appProps.banner)

    @Singleton
    fun koinLifecycleListener(
        closeCallbacks: List<AutoCloseable>,
        initCallbacks: List<BannerPrinter>
    ): LifecycleListener {
        return KoinLifecycleListener(closeCallbacks, initCallbacks)
    }

}
