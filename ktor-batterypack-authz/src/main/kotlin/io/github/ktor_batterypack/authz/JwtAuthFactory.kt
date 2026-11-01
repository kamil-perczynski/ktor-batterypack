package io.github.ktor_batterypack.authz

import io.ktor.server.auth.oidc.OidcToken
import io.ktor.utils.io.ExperimentalKtorApi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Creates [JwtAuth] from verified OIDC access tokens using claim names from [JwtAuthProps].
 */
class JwtAuthFactory(private val props: JwtAuthProps) {

    @OptIn(ExperimentalKtorApi::class)
    fun create(token: OidcToken.Access): JwtAuth {
        return create(token.claims.payload)
    }

    internal fun create(claims: JsonObject): JwtAuth {
        val id = stringValue(claims[props.idClaim])
        requireNotNull(id) { "Missing '${props.idClaim}' claim" }
        return JwtAuth(id = id, groups = groups(claims[props.groupsClaim]))
    }

    private fun groups(element: JsonElement?): Set<String> {
        if (element is JsonArray) {
            return element.mapNotNull { stringValue(it) }.toSet()
        }
        val group = stringValue(element) ?: return emptySet()
        return setOf(group)
    }

    private fun stringValue(element: JsonElement?): String? {
        if (element is JsonPrimitive && element.isString) {
            return element.content
        }
        return null
    }
}
