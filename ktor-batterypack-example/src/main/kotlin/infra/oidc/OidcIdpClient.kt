package io.github.kperczynski.infra.oidc

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.ktor_batterypack.core.ktor.client.pathPattern
import io.ktor.client.HttpClient
import io.ktor.client.request.basicAuth
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue

private val log = LoggerFactory.getLogger(OidcIdpClient::class.java)

@Singleton
class OidcIdpClient(
    private val props: OidcProps,
    private val jsonMapper: JsonMapper,
    @Named("oidc") private val httpClient: HttpClient,
) {

    suspend fun fetchUserinfo(accessToken: String): OidcUserinfo {
        val userinfo = httpClient.get("/oauth2/userInfo") {
            pathPattern("/oauth2/userInfo")
            headers.append("Authorization", "Bearer $accessToken")
        }

        val responseBody = userinfo.bodyAsText()

        if (!userinfo.status.isSuccess()) {
            log.error("Failed to fetch userinfo: status=${userinfo.status}, body=$responseBody")
            throw IdpHttpClientException("Failed to fetch userinfo: status=${userinfo.status}, body=$responseBody")
        }

        val parsed = try {
            jsonMapper.readValue<OidcUserinfo>(responseBody)
        } catch (e: Exception) {
            throw IdpHttpClientException("Failed to parse userinfo response: ${e.message}", e)
        }

        if (parsed.sub.isBlank()) {
            throw IdpHttpClientException("Userinfo response is missing sub claim")
        }

        return parsed
    }

    suspend fun refreshTokens(refreshToken: String): OidcTokens {
        val response = httpClient.post("/oauth2/token") {
            pathPattern("/oauth2/token")
            basicAuth(props.clientId, props.clientSecret)
            setBody(FormDataContent(Parameters.build {
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
            }))
        }

        val responseBody = response.bodyAsText()

        if (!response.status.isSuccess()) {
            log.error("Failed to refresh tokens: status=${response.status}, body=$responseBody")
            throw IdpHttpClientException("Failed to refresh tokens: status=${response.status}, body=$responseBody")
        }

        return try {
            jsonMapper.readValue<OidcTokens>(responseBody)
        } catch (e: Exception) {
            throw IdpHttpClientException("Failed to parse token response: ${e.message}", e)
        }
    }

    suspend fun revokeRefreshToken(refreshToken: String) {
        val response = httpClient.post("/oauth2/revoke") {
            pathPattern("/oauth2/revoke")
            basicAuth(props.clientId, props.clientSecret)
            setBody(FormDataContent(Parameters.build {
                append("token", refreshToken)
            }))
        }

        if (!response.status.isSuccess()) {
            log.error("Failed to revoke refresh token: status=${response.status}, body=${response.bodyAsText()}")
            throw IdpHttpClientException("Failed to revoke refresh token: status=${response.status}")
        }
    }
}

data class OidcTokens(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("expires_in") val expiresIn: Long,
    @JsonProperty("refresh_token") val refreshToken: String?,
)

data class OidcUserinfo(
    val sub: String,
    val username: String?,
    @JsonProperty("preferred_username") val preferredUsername: String?,
    val email: String?,
    @JsonProperty("email_verified") val emailVerified: String?,
    val identities: String?,
)
