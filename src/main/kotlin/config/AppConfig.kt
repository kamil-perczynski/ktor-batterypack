package io.github.kperczynski.config

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceOrFileSource
import com.sksamuel.hoplite.addResourceSource
import com.sksamuel.hoplite.sources.EnvironmentVariablesPropertySource
import kotlinx.serialization.Serializable

/**
 * Application configuration data class.
 * Uses Hoplite for type-safe configuration loading from YAML and environment variables.
 */
@Serializable
data class AppConfig(
    val ktor: KtorConfig = KtorConfig(),
    val banner: String? = null,
    val database: DatabaseConfig = DatabaseConfig()
)

@Serializable
data class KtorConfig(
    val deployment: DeploymentConfig = DeploymentConfig(),
    val application: KtorApplicationConfig = KtorApplicationConfig()
)

@Serializable
data class KtorApplicationConfig(
    val modules: List<String> = emptyList()
)

@Serializable
data class DeploymentConfig(
    val port: Int = 8080
)

@Serializable
data class DatabaseConfig(
    val url: String = "jdbc:postgresql://localhost:5432/ktordb",
    val user: String = "ktor",
    val password: String = "ktorpassword",
    val driver: String = "org.postgresql.Driver",
    val poolSize: Int = 2
)

/**
 * Loads the application configuration using Hoplite.
 * Supports loading from:
 * 1. Environment variables (prefixed with APP_)
 * 2. application.yaml on classpath
 * 3. application-local.yaml on classpath (if exists, for local development)
 */
fun loadConfig(): AppConfig {
    return ConfigLoaderBuilder.default()
        .addSource(EnvironmentVariablesPropertySource(useUnderscoresAsSeparator = true, allowUppercaseNames = true))
        .addResourceSource("/application.yaml")
        .addResourceSource("/application-local.yaml", optional = true)
        .build()
        .loadConfigOrThrow()
}

