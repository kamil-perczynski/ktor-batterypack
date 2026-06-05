package io.github.ktor_batterypack.core.exception

/**
 * Represents a domain error with a stable code and a human-readable message.
 */
interface ErrorCode {
    val code: String
    val message: String
}
