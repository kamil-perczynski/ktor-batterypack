package io.github.ktor_batterypack.core.ktor.client

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

/**
 * Contract for plugins that can be installed into every [HttpClient] built by the shared
 * [KtorHttpClientFactory], without the factory depending on concrete plugin implementations.
 *
 * Implementations are typically `@Singleton` beans bound via a Koin module (they must implicitly
 * bind to this interface), and are collected by the factory constructor through the DI container.
 */
interface KtorHttpClientPlugin {
    /**
     * Installs this plugin into the given [HttpClientConfig] builder.
     */
    fun register(config: HttpClientConfig<*>)
}
