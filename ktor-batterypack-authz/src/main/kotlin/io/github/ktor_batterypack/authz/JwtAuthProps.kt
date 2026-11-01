package io.github.ktor_batterypack.authz

import com.fasterxml.jackson.annotation.JsonPropertyDescription

/**
 * JWT claim names used to resolve [JwtAuth].
 */
data class JwtAuthProps(
    @param:JsonPropertyDescription("JWT claim holding the subject identifier")
    val idClaim: String = "sub",
    @param:JsonPropertyDescription("JWT claim holding the list of user groups")
    val groupsClaim: String = "groups",
)
