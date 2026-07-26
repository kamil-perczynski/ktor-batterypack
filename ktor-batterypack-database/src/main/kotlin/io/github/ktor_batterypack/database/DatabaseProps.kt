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
    val poolSize: Int = 2
)
