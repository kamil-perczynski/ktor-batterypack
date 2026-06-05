package io.github.ktor_batterypack.core.exception

class ResourceMissingException(
    val clazz: Class<out Any>,
    val identifier: Any,
    val identifierType: String = "id"
) :
    RuntimeException(
        "${clazz.simpleName} with $identifierType: $identifier is missing"
    ) {
    companion object {
        val ERROR_CODE = "RESOURCE_MISSING"
    }
}
