package io.github.ktor_batterypack.database.liquibase

import com.fasterxml.jackson.annotation.JsonPropertyDescription

data class LiquibaseProps(
    @param:JsonPropertyDescription("Path to the Liquibase changelog file")
    val changeLogPath: String = "db/changelog/changelog.xml",
    @param:JsonPropertyDescription("Comma-separated Liquibase contexts to execute")
    val contexts: String = "",
    @param:JsonPropertyDescription("Liquibase label expression to filter changesets")
    val labels: String = "",
    @param:JsonPropertyDescription("Default schema name for Liquibase operations")
    val defaultSchemaName: String? = null,
    @param:JsonPropertyDescription("Schema where Liquibase stores its DATABASECHANGELOG tables")
    val liquibaseSchemaName: String? = null,
    @param:JsonPropertyDescription("Name of the table Liquibase uses to track change history")
    val databaseChangeLogTableName: String? = null,
    @param:JsonPropertyDescription("Name of the table Liquibase uses to coordinate concurrent access")
    val databaseChangeLogLockTableName: String? = null,
    @param:JsonPropertyDescription("Changelog parameters passed to Liquibase")
    val parameters: Map<String, String> = emptyMap(),
    @param:JsonPropertyDescription("Drop all database objects before running migrations")
    val dropFirst: Boolean = false
)
