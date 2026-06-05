package io.github.ktor_batterypack.core.exception

/**
 * Thrown when a requested resource cannot be found by the given identifier.
 */
class ResourceMissingException(
    val clazz: Class<out Any>,
    val identifier: Any,
    val identifierType: String = "id"
) :
    RuntimeException(
        "${clazz.simpleName} with $identifierType: $identifier is missing"
    ) {

    /**
     * Holds the stable error code for missing resources.
     */
    companion object {
        const val ERROR_CODE = "RESOURCE_MISSING"
    }
}
