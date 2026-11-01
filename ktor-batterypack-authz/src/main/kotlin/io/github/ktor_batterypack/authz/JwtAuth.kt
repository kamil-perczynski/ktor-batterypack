package io.github.ktor_batterypack.authz

/**
 * [Auth] resolved from JWT claims.
 */
data class JwtAuth(
    override val id: String,
    override val groups: Set<String>,
) : Auth
