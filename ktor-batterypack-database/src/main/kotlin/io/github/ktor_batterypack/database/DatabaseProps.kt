package io.github.ktor_batterypack.database

data class DatabaseProps(
    val url: String = "",
    val user: String = "",
    val password: String = "",
    val driver: String = "org.postgresql.Driver",
    val poolSize: Int = 2
)
