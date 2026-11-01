package io.github.kperczynski.infra.oidc

import io.ktor.server.auth.oidc.OidcProviderConfig
import io.ktor.utils.io.ExperimentalKtorApi

@OptIn(ExperimentalKtorApi::class)
fun interface OidcProviderCustomizer {
    fun customize(config: OidcProviderConfig)
}
