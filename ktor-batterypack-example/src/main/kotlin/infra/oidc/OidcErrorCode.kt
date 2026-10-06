package io.github.kperczynski.infra.oidc

import io.github.ktor_batterypack.core.exception.ErrorCode

enum class OidcErrorCode(override val message: String) : ErrorCode {
    REFRESH_TOKEN_MISSING("Refresh token is missing"),
    ACCESS_TOKEN_MISSING("Access token is missing");

    override val code: String
        get() = name
}
