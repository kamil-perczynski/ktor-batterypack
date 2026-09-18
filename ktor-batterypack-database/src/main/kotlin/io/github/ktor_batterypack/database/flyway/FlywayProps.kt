package io.github.ktor_batterypack.database.flyway

import com.fasterxml.jackson.annotation.JsonPropertyDescription

data class FlywayProps(
    @param:JsonPropertyDescription("Comma-separated locations of Flyway migration scripts")
    val locations: String = "classpath:db/migration",
    @param:JsonPropertyDescription("Target version up to which migrations run")
    val target: String? = null,
    @param:JsonPropertyDescription("Default schema for Flyway operations")
    val defaultSchema: String? = null,
    @param:JsonPropertyDescription("Comma-separated schemas managed by Flyway")
    val schemas: String? = null,
    @param:JsonPropertyDescription("Placeholders for migration scripts")
    val placeholders: Map<String, String> = emptyMap(),
    @param:JsonPropertyDescription("Drop all database objects before running migrations")
    val cleanBeforeMigrate: Boolean = false
)
