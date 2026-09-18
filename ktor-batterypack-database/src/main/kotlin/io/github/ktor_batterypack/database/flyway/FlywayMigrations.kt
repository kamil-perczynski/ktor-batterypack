package io.github.ktor_batterypack.database.flyway

import io.github.ktor_batterypack.core.di.BootstrapCallback
import org.flywaydb.core.Flyway
import javax.sql.DataSource

/**
 * Runs Flyway migrations against the configured [DataSource] on startup.
 *
 * Implements [BootstrapCallback] so the application lifecycle invokes it once during
 * startup. Locations, schemas, target version, placeholders and
 * clean-before-migrate behaviour are read from [FlywayProps].
 */
class FlywayMigrations(
    private val props: FlywayProps,
    private val dataSource: DataSource,
) : BootstrapCallback {

    /**
     * Builds the Flyway configuration from [FlywayProps], optionally cleans the
     * schema, and applies all pending migrations.
     */
    override fun onBootstrap() {
        val config = Flyway.configure()
            .dataSource(dataSource)
            .locations(*props.locations.split(',').map { it.trim() }.toTypedArray())
            .placeholders(props.placeholders)

        val schemas = props.schemas
        if (schemas != null && schemas.isNotBlank()) {
            config.schemas(*schemas.split(',').map { it.trim() }.toTypedArray())
        }

        val defaultSchema = props.defaultSchema
        if (defaultSchema != null) {
            config.defaultSchema(defaultSchema)
        }

        val target = props.target
        if (target != null) {
            config.target(target)
        }

        if (props.cleanBeforeMigrate) {
            config.cleanDisabled(false)
        }

        val flyway = config.load()

        if (props.cleanBeforeMigrate) {
            flyway.clean()
        }

        flyway.migrate()
    }
}
