package io.github.ktor_batterypack.core.config

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import com.sksamuel.hoplite.addResourceOrFileSource
import com.sksamuel.hoplite.sources.EnvironmentVariablesPropertySource
import com.sksamuel.hoplite.sources.SystemPropertiesPropertySource

@OptIn(ExperimentalHoplite::class)
inline fun <reified T> loadConfig(profiles: List<String>, includeSystemProperties: Boolean = true): T {
    val source = ConfigLoaderBuilder.empty()
        .addDefaultDecoders()
        .addDefaultParsers()
        .addDefaultPreprocessors()
        .addDefaultNodeTransformers()
        .addDefaultParamMappers()
        .addSource(
            EnvironmentVariablesPropertySource(
                useUnderscoresAsSeparator = true,
                allowUppercaseNames = true
            )
        )

    if (includeSystemProperties) {
        source.addSource(SystemPropertiesPropertySource())
    }

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
