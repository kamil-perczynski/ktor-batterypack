package io.github.ktor_batterypack.authz

/**
 * Role-based permission checks. Permissions of an [Auth] are the union of permissions granted to its groups.
 */
class PermissionChecker(props: AuthzProps) {

    private val permissionsByGroup: Map<String, Set<String>> =
        props.groups.mapValues { it.value.permissions.toSet() }

    fun findPermissions(auth: Auth): Set<String> {
        return auth.groups.flatMapTo(mutableSetOf()) { permissionsByGroup[it].orEmpty() }
    }

    fun hasPermission(auth: Auth, permission: String): Boolean {
        return auth.groups.any { permissionsByGroup[it]?.contains(permission) == true }
    }

    fun checkPermission(auth: Auth, permission: String) {
        if (!hasPermission(auth, permission)) {
            throw PermissionDeniedException(permission)
        }
    }
}
