package io.github.ktor_batterypack.authz

import com.fasterxml.jackson.annotation.JsonPropertyDescription

/**
 * Authorization group configuration.
 */
data class AuthzGroupProps(
    @param:JsonPropertyDescription("Permissions granted to the group, in noun:action format")
    val permissions: List<String> = emptyList(),
)
