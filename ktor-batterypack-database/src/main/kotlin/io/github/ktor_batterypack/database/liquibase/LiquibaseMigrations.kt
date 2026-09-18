package io.github.ktor_batterypack.database.liquibase

import io.github.ktor_batterypack.core.di.BootstrapCallback
import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.Scope
import liquibase.UpdateSummaryEnum
import liquibase.UpdateSummaryOutputEnum
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.ui.LoggerUIService
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val log = LoggerFactory.getLogger(LiquibaseMigrations::class.java)

/**
 * Runs Liquibase migrations against the configured [DataSource] on startup.
 *
 * Implements [BootstrapCallback] so the application lifecycle invokes it once during
 * startup. Changelog location, contexts, labels, schema settings and changelog
 * parameters are read from [LiquibaseProps].
 */
class LiquibaseMigrations(
    private val props: LiquibaseProps,
    private val dataSource: DataSource,
) : BootstrapCallback {

    /**
     * Opens a connection, resolves the Liquibase database and applies all
     * pending changesets from the configured changelog.
     */
    override fun onBootstrap() {
        log.info("Running Liquibase migrations from changelog: {}", props.changeLogPath)
        dataSource.connection.use { conn ->
            val database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(JdbcConnection(conn))

            if (props.defaultSchemaName != null) {
                database.defaultSchemaName = props.defaultSchemaName
            }
            if (props.liquibaseSchemaName != null) {
                database.liquibaseSchemaName = props.liquibaseSchemaName
            }
            if (props.databaseChangeLogTableName != null) {
                database.databaseChangeLogTableName = props.databaseChangeLogTableName
            }
            if (props.databaseChangeLogLockTableName != null) {
                database.databaseChangeLogLockTableName = props.databaseChangeLogLockTableName
            }

            val scopeValues = mapOf<String, Any>(Scope.Attr.ui.name to LoggerUIService())
            Scope.child(scopeValues, {
                val liquibase =
                    Liquibase(props.changeLogPath, ClassLoaderResourceAccessor(), database)

                liquibase.use { liquibase ->
                    liquibase.setShowSummaryOutput(UpdateSummaryOutputEnum.LOG)
                    liquibase.setShowSummary(UpdateSummaryEnum.SUMMARY)

                    liquibase.validate()

                    props.parameters.forEach { (key, value) ->
                        liquibase.setChangeLogParameter(
                            key,
                            value
                        )
                    }

                    if (props.dropFirst) {
                        log.warn("dropFirst is enabled, dropping all database objects before migrating")
                        liquibase.dropAll()
                    }

                    liquibase.update(Contexts(props.contexts), LabelExpression(props.labels))
                }
            })
        }
    }
}
