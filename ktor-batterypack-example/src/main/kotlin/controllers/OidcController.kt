@file:OptIn(ExperimentalKtorApi::class)

package io.github.kperczynski.controllers

import io.github.kperczynski.infra.oidc.IdpHttpClientException
import io.github.kperczynski.infra.oidc.OidcAuthCookies
import io.github.kperczynski.infra.oidc.OidcErrorCode
import io.github.kperczynski.infra.oidc.OidcIdpClient
import io.github.kperczynski.infra.oidc.OidcUserinfo
import io.github.ktor_batterypack.core.exception.ErrorCodeException
import io.github.ktor_batterypack.core.ktor.KtorController
import io.github.ktor_batterypack.core.ktor.currentRoutingCall
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.oidc.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory
import pl.kperczynski.florin.rest.AuthApi
import pl.kperczynski.florin.rest.dto.AckDto
import pl.kperczynski.florin.rest.dto.AuthDto
import pl.kperczynski.florin.rest.dto.AuthInfoDto
import pl.kperczynski.florin.rest.dto.AuthInitParamsParameterDto
import pl.kperczynski.florin.rest.dto.AuthProviderDto
import pl.kperczynski.florin.rest.dto.ForgotPasswordConfirmRequestDto
import pl.kperczynski.florin.rest.dto.ForgotPasswordInitRequestDto
import pl.kperczynski.florin.rest.dto.RegisterUserConfirmRequestDto
import pl.kperczynski.florin.rest.dto.RegisterUserInitRequestDto
import tools.jackson.databind.json.JsonMapper

private val log = LoggerFactory.getLogger(OidcController::class.java)

@Singleton
class OidcController(
    private val jsonMapper: JsonMapper,
    private val oidcProvider: OidcProvider,
    private val idpClient: OidcIdpClient,
    private val authCookies: OidcAuthCookies,
) : KtorController, AuthApi {

    override fun register(routing: Routing) {
        routing.get("/auth/sign-out") {
            signOut()
            call.respondRedirect("/")
        }

        routing.get("/auth/refresh-auth") {
            val res = refreshAuth()
            call.respond(HttpStatusCode.OK, res)
        }

        routing.get("/auth/idp-callback") {
            val res = idpCallback(
                code = call.request.queryParameters["code"],
                state = call.request.queryParameters["state"]
            )
            call.respondText(res, ContentType.Text.Html)
        }

        routing.get("/auth/auth-init") {
            authInit(
                AuthInitParamsParameterDto(
                    extendedScope = call.request.queryParameters["extendedScope"]?.toBoolean(),
                    provider = call.request.queryParameters["provider"]?.let { AuthProviderDto.valueOf(it) }
                )
            )
        }

        routing.post("/auth/classic-auth") {
            val res = classicAuth(
                email = call.receiveParameters()["email"] ?: "",
                password = call.receiveParameters()["password"] ?: "",
                increasedPrivileges = call.receiveParameters()["increasedPrivileges"]?.toBoolean()
            )
            call.respond(HttpStatusCode.OK, res)
        }

        routing.post("/auth/forgot-password-init") {
            val dto = call.receive<ForgotPasswordInitRequestDto>()
            val res = forgotPasswordInit(dto)
            call.respond(HttpStatusCode.OK, res)
        }

        routing.post("/auth/forgot-password-confirm") {
            val dto = call.receive<ForgotPasswordConfirmRequestDto>()
            val res = forgotPasswordConfirm(dto)
            call.respond(HttpStatusCode.OK, res)
        }

        routing.post("/auth/register-user-init") {
            val dto = call.receive<RegisterUserInitRequestDto>()
            val res = registerUserInit(dto)
            call.respond(HttpStatusCode.OK, res)
        }

        routing.post("/auth/register-user-confirm") {
            val dto = call.receive<RegisterUserConfirmRequestDto>()
            val res = registerUserConfirm(dto)
            call.respond(HttpStatusCode.OK, res)
        }

        routing.authenticateWith(oidcProvider.jwtBearer) {
            get("/auth/user-info") {
                val authInfo = fetchUserInfo()
                call.respond(HttpStatusCode.OK, authInfo)
            }
        }
    }

    override suspend fun authInit(params: AuthInitParamsParameterDto?) {
        TODO("Not yet implemented")
    }

    override suspend fun classicAuth(
        email: String,
        password: String,
        increasedPrivileges: Boolean?
    ): AckDto {
        TODO("Not yet implemented")
    }

    override suspend fun fetchUserInfo(): AuthInfoDto {
        val call = currentRoutingCall()
        val accessToken = call.principal<OidcToken.Access>()
            ?: throw ErrorCodeException(OidcErrorCode.ACCESS_TOKEN_MISSING)

        val refreshToken = authCookies.readRefreshToken(call.request)
        val userinfo = idpClient.fetchUserinfo(accessToken.value)
        return toAuthInfo(userinfo, accessToken.claims, refreshToken)
    }

    override suspend fun forgotPasswordConfirm(forgotPasswordConfirmRequestDto: ForgotPasswordConfirmRequestDto): AckDto {
        TODO("Not yet implemented")
    }

    override suspend fun forgotPasswordInit(forgotPasswordInitRequestDto: ForgotPasswordInitRequestDto): AckDto {
        TODO("Not yet implemented")
    }

    override suspend fun idpCallback(code: String?, state: String?): String {
        TODO("Not yet implemented")
    }

    override suspend fun refreshAuth(): AckDto {
        val call = currentRoutingCall()
        val refreshToken = authCookies.readRefreshToken(call.request)
            ?: throw ErrorCodeException(OidcErrorCode.REFRESH_TOKEN_MISSING)

        val tokens = idpClient.refreshTokens(refreshToken)

        authCookies.write(call, tokens.accessToken, tokens.refreshToken, tokens.expiresIn)

        return AckDto(ok = true)
    }

    override suspend fun registerUserConfirm(registerUserConfirmRequestDto: RegisterUserConfirmRequestDto): AckDto {
        TODO("Not yet implemented")
    }

    override suspend fun registerUserInit(registerUserInitRequestDto: RegisterUserInitRequestDto): AckDto {
        TODO("Not yet implemented")
    }

    override suspend fun signOut() {
        val call = currentRoutingCall()
        val refreshToken = authCookies.readRefreshToken(call.request)

        if (refreshToken != null) {
            try {
                idpClient.revokeRefreshToken(refreshToken)
            } catch (e: IdpHttpClientException) {
                log.warn("Proceeding with local sign-out despite revocation failure: {}", e.message)
            }
        }

        authCookies.clear(call)
    }

}

private fun toAuthInfo(
    userInfo: OidcUserinfo,
    claims: TokenClaims,
    refreshToken: String?,
): AuthInfoDto {
    val now = System.currentTimeMillis() / 1000
    val exp = claims.expiresAt

    val groups = claims.claim("cognito:groups")?.jsonArray
        ?.map { it.jsonPrimitive.content }
        ?.toList()

    return AuthInfoDto(
        auth = AuthDto(
            sub = userInfo.sub,
            username = userInfo.username ?: "",
            preferredUsername = userInfo.preferredUsername ?: "",
            email = userInfo.email ?: ""
        ),
        soonToExpire = exp != null && exp.epochSeconds < now + 300,
        expired = exp != null && exp.epochSeconds < now,
        refreshPossible = !refreshToken.isNullOrBlank(),
        privileged = groups?.contains("admins") ?: false
    )
}


