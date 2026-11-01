package io.github.ktor_batterypack.authz

/**
 * Authenticated identity used for permission checks.
 */
interface Auth {
    val id: String
    val groups: Set<String>
}
