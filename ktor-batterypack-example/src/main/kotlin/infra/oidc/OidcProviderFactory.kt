package io.github.kperczynski.infra.oidc

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.oidc.Oidc
import io.ktor.server.auth.oidc.OidcProvider
import io.ktor.server.auth.oidc.OidcStateEncryptionKey
import io.ktor.utils.io.ExperimentalKtorApi
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Singleton

@Singleton
class OidcProviderFactory(
    private val props: OidcProps,
    private val callbacks: OidcCallbacks,
    private val app: Application
) {

    @OptIn(ExperimentalKtorApi::class)
    fun create(): OidcProvider = runBlocking {
        app.install(Oidc).identityProvider("cognito") {
            issuer = props.issuer

            bearer {
                audience = props.audience.toSet()
                tokenExtractor = { call.request.cookies[props.accessTokenCookieName] }
            }

            oauth {
                clientId = props.clientId
                clientSecret = props.clientSecret
                resourceIndicators = props.audience
                stateEncryptionKey =
                    props.stateEncryptionKey?.let { OidcStateEncryptionKey.of(it.toByteArray()) }
                disableSessions()

                onAuthenticated { oidcId ->
                    callbacks.onAuthenticated(call, oidcId)
                }
            }
        }
    }
}
