package io.github.ktor_batterypack.database

import com.fasterxml.jackson.annotation.JsonPropertyDescription

data class DatabaseProps(
    @param:JsonPropertyDescription("JDBC connection URL")
    val url: String = "",
    @param:JsonPropertyDescription("Database user")
    val user: String = "",
    @param:JsonPropertyDescription("Database password")
    val password: String = "",
    @param:JsonPropertyDescription("JDBC driver class name")
    val driver: String = "org.postgresql.Driver",
    @param:JsonPropertyDescription("Connection pool size")
    val poolSize: Int = 2,
    @param:JsonPropertyDescription("Minimum idle connections in pool")
    val minimumIdle: Int? = null,
    @param:JsonPropertyDescription("Connection auto-commit")
    val autoCommit: Boolean = true,
    @param:JsonPropertyDescription("Transaction isolation level")
    val transactionIsolation: String = "TRANSACTION_REPEATABLE_READ",
    @param:JsonPropertyDescription("Connection acquisition timeout in milliseconds")
    val connectionTimeoutMs: Long = 30000,
    @param:JsonPropertyDescription("Connection validation timeout in milliseconds")
    val validationTimeoutMs: Long = 5000,
    @param:JsonPropertyDescription("Idle connection timeout in milliseconds")
    val idleTimeoutMs: Long = 600000,
    @param:JsonPropertyDescription("Maximum connection lifetime in milliseconds")
    val maxLifetimeMs: Long = 1800000,
    @param:JsonPropertyDescription("Keepalive interval in milliseconds")
    val keepaliveTimeMs: Long = 0,
    @param:JsonPropertyDescription("Connection leak detection threshold in milliseconds")
    val leakDetectionThresholdMs: Long = 0,
    @param:JsonPropertyDescription("Connection pool name")
    val poolName: String? = null,
    @param:JsonPropertyDescription("Arbitrary properties passed to the JDBC DataSource/Driver")
    val dataSourceProperties: Map<String, String> = emptyMap()
)
