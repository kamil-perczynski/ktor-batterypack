package io.github.kperczynski.infra.oidc

import io.ktor.server.auth.oidc.OidcProviderConfig
import io.ktor.server.auth.oidc.OpenIdProviderMetadata
import io.ktor.server.auth.oidc.OpenIdTestKeys
import io.ktor.utils.io.ExperimentalKtorApi
import org.koin.core.annotation.Singleton

@OptIn(ExperimentalKtorApi::class)
@Singleton
class TestOidcProviderCustomizer(
    private val props: OidcProps,
    private val keys: OpenIdTestKeys
) : OidcProviderCustomizer {

    override fun customize(config: OidcProviderConfig) {
        config.metadata = OpenIdProviderMetadata(
            issuer = props.issuer,
            authorizationEndpoint = "${props.issuer}/authorize",
            tokenEndpoint = "${props.issuer}/token",
            jwksUri = "${props.issuer}/jwks",
        )
        config.jwt(keys)
    }
}
