package io.github.ktor_batterypack.validation.example

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class AppProps(
    val port: Int = 8080,
    val databaseProps: DatabaseProps = DatabaseProps(),
    val ktor: KtorProps = KtorProps()
)

data class DatabaseProps(
    val host: String = "localhost",
    val port: Int = 5432,
    @field:NotEmpty
    val connectionProps: Map<String, DatabaseProps> = emptyMap()
)

data class KtorProps(
    @field:NotEmpty
    val modules: List<@NotBlank String> = emptyList(),
)