package io.github.kperczynski.controllers

import io.github.kperczynski.domain.user.User
import io.github.kperczynski.domain.user.UserRepo
import io.github.kperczynski.infra.ACCESS_TOKEN_COOKIE
import io.github.kperczynski.infra.ADMINS
import io.github.kperczynski.infra.EDITORS
import io.github.kperczynski.infra.KtorBatteriesIT
import io.github.kperczynski.infra.READERS
import io.github.kperczynski.infra.oidc.OidcProps
import io.ktor.client.request.cookie
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.oidc.OpenIdTestKeys
import io.ktor.utils.io.ExperimentalKtorApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.koin.ktor.plugin.koin
import tools.jackson.databind.json.JsonMapper
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalKtorApi::class)
class AuthorizationIT : KtorBatteriesIT() {

    private val userRepo: UserRepo = application.koin().get()
    private val jsonMapper: JsonMapper = application.koin().get()
    private val oidcProps: OidcProps = application.koin().get()

    @Test
    fun `should return 401 problem detail without access token`() = runTest {
        val response = httpClient.get("/api/plants")

        assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
        assertThat(response.headers[HttpHeaders.WWWAuthenticate]).startsWith("Bearer")
        assertThat(jsonMapper.readTree(response.bodyAsText())).isEqualTo(
            jsonMapper.readTree(
                """
                {
                  "type": "UNAUTHORIZED",
                  "title": "Unauthorized",
                  "status": 401,
                  "detail": "Authentication is required to access this resource",
                  "instance": "/api/plants"
                }
                """
            )
        )
    }

    @Test
    fun `should return 401 for expired access token`() = runTest {
        val expiredToken = openIdTestKeys.accessToken {
            subject = "test-user"
            issuedAt = Clock.System.now().minus(30.minutes)
            expiresAt = Clock.System.now().minus(10.minutes)
            claim("cognito:groups", listOf(ADMINS))
        }

        val response = httpClient.get("/api/plants") {
            cookie(ACCESS_TOKEN_COOKIE, expiredToken)
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
    }

    @Test
    fun `should return 401 for access token signed with foreign key`() = runTest {
        val foreignKeys = OpenIdTestKeys.rsa(issuer = oidcProps.issuer, audience = oidcProps.audience.first())
        val foreignToken = foreignKeys.accessToken {
            subject = "test-user"
            claim("cognito:groups", listOf(ADMINS))
        }

        val response = httpClient.get("/api/plants") {
            cookie(ACCESS_TOKEN_COOKIE, foreignToken)
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
    }

    @Test
    fun `should return 403 problem detail when group lacks permission`() = runTest {
        val user = userRepo.create(User(id = 0u, name = "Grace", age = 33))

        val response = httpClient.delete("/users/${user.id}") {
            cookie(ACCESS_TOKEN_COOKIE, accessToken(READERS))
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.Forbidden)
        assertThat(jsonMapper.readTree(response.bodyAsText())).isEqualTo(
            jsonMapper.readTree(
                """
                {
                  "type": "PERMISSION_DENIED",
                  "title": "Forbidden",
                  "status": 403,
                  "detail": "Permission 'user:delete' denied",
                  "instance": "/users/${user.id}",
                  "permission": "user:delete"
                }
                """
            )
        )
    }

    @Test
    fun `should return 403 for token without groups`() = runTest {
        val response = httpClient.get("/api/plants") {
            cookie(ACCESS_TOKEN_COOKIE, accessToken())
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.Forbidden)
    }

    @Test
    fun `should allow editors to read but not to top up wallets`() = runTest {
        val readResponse = httpClient.get("/api/plants") {
            cookie(ACCESS_TOKEN_COOKIE, accessToken(EDITORS))
        }
        val topupResponse = httpClient.get("/api/wallets/topup?amount=10.00&walletId=1") {
            cookie(ACCESS_TOKEN_COOKIE, accessToken(EDITORS))
        }

        assertThat(readResponse.status).isEqualTo(HttpStatusCode.OK)
        assertThat(topupResponse.status).isEqualTo(HttpStatusCode.Forbidden)
    }

    @Test
    fun `should allow admins to delete users`() = runTest {
        val user = userRepo.create(User(id = 0u, name = "Heidi", age = 44))

        val response = httpClient.delete("/users/${user.id}") {
            cookie(ACCESS_TOKEN_COOKIE, accessToken(ADMINS))
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.NoContent)
    }

}
