@file:OptIn(ExperimentalKtorApi::class)

package io.github.kperczynski.infra.oidc

import io.ktor.server.auth.oidc.OidcToken
import io.ktor.server.auth.oidc.TokenClaims
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.RoutingCall
import io.ktor.utils.io.ExperimentalKtorApi
import org.koin.core.annotation.Singleton
import java.time.Duration
import kotlin.time.toJavaInstant

@Singleton
class OidcCallbacks(
    private val props: OidcProps,
    private val authCookies: OidcAuthCookies,
) {
    suspend fun onAuthenticated(call: RoutingCall, oidcId: OidcToken.Id) {
        authCookies.write(call, oidcId.accessToken, oidcId.refreshToken, toMaxAge(oidcId.claims))
        call.respondRedirect("/")
    }
}

private fun toMaxAge(claims: TokenClaims): Long {
    val expiresAt = claims.expiresAt!!.toJavaInstant()
    val issuedAt = claims.issuedAt!!.toJavaInstant()
    return Duration.between(issuedAt, expiresAt).seconds
}
