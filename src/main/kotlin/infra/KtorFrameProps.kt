package io.github.kperczynski.infra

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import com.sksamuel.hoplite.addResourceOrFileSource
import com.sksamuel.hoplite.addResourceSource
import com.sksamuel.hoplite.sources.EnvironmentVariablesPropertySource
import io.github.kperczynski.infra.client.FlorinClientProps
import io.github.kperczynski.libs.db.DatabaseProps
import io.github.kperczynski.libs.ktor.KtorProps
import io.github.kperczynski.libs.ktor.multipart.MultipartProps

/**
 * Application configuration data class.
 * Uses Hoplite for type-safe configuration loading from YAML and environment variables.
 */
data class AppProps(
    val ktor: KtorProps = KtorProps(),
    val banner: String? = null,
    val database: DatabaseProps = DatabaseProps(),
    val florin: FlorinClientProps = FlorinClientProps(),
    val multipart: MultipartProps = MultipartProps()
)

@OptIn(ExperimentalHoplite::class)
fun loadConfig(profiles: List<String>): AppProps {
    val source = ConfigLoaderBuilder.default()
        .addSource(
            EnvironmentVariablesPropertySource(
                useUnderscoresAsSeparator = true,
                allowUppercaseNames = true
            )
        )

    for (profile in profiles.reversed()) {
        source.addFileSource("application-$profile.yaml", optional = true)
    }

    for (profile in profiles.reversed()) {
        source.addResourceOrFileSource("/application-$profile.yaml", optional = true)
    }

    source.addFileSource("application.yaml", optional = true)
    source.addResourceOrFileSource("/application.yaml", optional = true)

    return source
        .withExplicitSealedTypes()
        .build()
        .loadConfigOrThrow()
}
