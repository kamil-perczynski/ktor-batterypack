package io.github.kperczynski.infra.oidc

import io.github.ktor_batterypack.authz.JwtAuth
import io.github.ktor_batterypack.authz.JwtAuthFactory
import io.ktor.server.auth.SimpleAuthenticationScheme
import io.ktor.server.auth.mapPrincipal
import io.ktor.server.auth.oidc.OidcProvider
import io.ktor.utils.io.ExperimentalKtorApi
import org.koin.core.annotation.Singleton

@OptIn(ExperimentalKtorApi::class)
@Singleton
class OidcAuthScheme(oidcProvider: OidcProvider, jwtAuthFactory: JwtAuthFactory) {

    val jwt: SimpleAuthenticationScheme<JwtAuth> =
        oidcProvider.jwtBearer.mapPrincipal { token -> jwtAuthFactory.create(token) }

}
