package io.github.kperczynski.infra

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addResourceSource
import com.sksamuel.hoplite.sources.EnvironmentVariablesPropertySource
import io.github.kperczynski.libs.db.DatabaseProps
import io.github.kperczynski.libs.ktor.KtorProps

/**
 * Application configuration data class.
 * Uses Hoplite for type-safe configuration loading from YAML and environment variables.
 */
data class AppProps(
    val ktor: KtorProps = KtorProps(),
    val banner: String? = null,
    val database: DatabaseProps = DatabaseProps()
)

@OptIn(ExperimentalHoplite::class)
fun loadConfig(): AppProps {
    return ConfigLoaderBuilder.default()
        .addSource(EnvironmentVariablesPropertySource(useUnderscoresAsSeparator = true, allowUppercaseNames = true))
        .addResourceSource("/application.yaml")
        .addResourceSource("/application-local.yaml", optional = true)
        .withExplicitSealedTypes()
        .build()
        .loadConfigOrThrow()
}
