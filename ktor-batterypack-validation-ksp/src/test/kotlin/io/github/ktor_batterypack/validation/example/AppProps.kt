package io.github.ktor_batterypack.validation.example

import jakarta.validation.constraints.NotEmpty

data class AppProps(
    val port: Int = 8080,
    @field:NotEmpty
    val modules: List<String>,
    val databaseProps: DatabaseProps = DatabaseProps()
)

data class DatabaseProps(
    val host: String = "localhost",
    val port: Int = 5432,
    @field:NotEmpty
    val connectionProps: Map<String, DatabaseProps> = emptyMap()
)
