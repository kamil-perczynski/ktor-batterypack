package io.github.kperczynski.infra.oidc

data class OidcProps(
    val enabled: Boolean = false,
    val issuer: String,
    val audience: List<String> = emptyList(),
    val clientId: String,
    val clientSecret: String,
    val baseUrl: String,
    val connectTimeoutMs: Long = 250L,
    val readTimeoutMs: Long = 1000L,
    val stateEncryptionKey: String? = null,
    val accessTokenCookieName: String = "access_token",
    val refreshTokenCookieName: String = "refresh_token",

    val groupsClaim: String = "cognito:groups",
    val privilegedGroupName: String = "admins",
)
