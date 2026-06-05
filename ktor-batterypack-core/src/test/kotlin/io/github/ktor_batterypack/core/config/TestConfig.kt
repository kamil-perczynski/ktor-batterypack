package io.github.ktor_batterypack.core.config

data class TestConfig(
    val app: AppConfig = AppConfig(),
    val database: DatabaseConfig = DatabaseConfig()
)

data class AppConfig(
    val name: String = "",
    val port: Int = 0
)

data class DatabaseConfig(
    val url: String = "",
    val user: String = "",
    val driver: String = "",
    val poolSize: Int = 0
)
