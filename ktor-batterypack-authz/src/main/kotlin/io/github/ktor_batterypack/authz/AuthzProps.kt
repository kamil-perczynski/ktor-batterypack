package io.github.ktor_batterypack.authz

import com.fasterxml.jackson.annotation.JsonPropertyDescription

/**
 * Authorization configuration: JWT claims mapping and authorization groups.
 */
data class AuthzProps(
    @param:JsonPropertyDescription("JWT claims mapping")
    val jwt: JwtAuthProps = JwtAuthProps(),
    @param:JsonPropertyDescription("Authorization groups keyed by group name")
    val groups: Map<String, AuthzGroupProps> = emptyMap(),
)
