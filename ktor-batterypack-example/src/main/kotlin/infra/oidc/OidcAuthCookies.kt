package io.github.kperczynski.infra.oidc

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ApplicationRequest
import org.koin.core.annotation.Singleton

@Singleton
class OidcAuthCookies(private val props: OidcProps) {

    fun write(call: ApplicationCall, accessToken: String, refreshToken: String?, maxAgeSeconds: Long) {
        append(call, props.accessTokenCookieName, accessToken, maxAgeSeconds)
        if (!refreshToken.isNullOrBlank()) {
            append(call, props.refreshTokenCookieName, refreshToken, maxAgeSeconds)
        }
    }

    fun clear(call: ApplicationCall) {
        append(call, props.accessTokenCookieName, "", 0)
        append(call, props.refreshTokenCookieName, "", 0)
    }

    fun readRefreshToken(request: ApplicationRequest): String? = request.cookies[props.refreshTokenCookieName]

    private fun append(call: ApplicationCall, name: String, value: String, maxAgeSeconds: Long) {
        call.response.cookies.append(
            name,
            value,
            maxAge = maxAgeSeconds,
            secure = call.request.local.scheme == "https",
            httpOnly = true,
            path = "/",
            extensions = mapOf("SameSite" to "Lax")
        )
    }
}
