package io.github.ktor_batterypack.authz

/**
 * Thrown when an [Auth] lacks a required permission. Mapped to HTTP 403.
 */
class PermissionDeniedException(val permission: String) :
    RuntimeException("Permission '$permission' denied")
