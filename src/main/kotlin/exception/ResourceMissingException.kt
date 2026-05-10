package io.github.kperczynski.exception

class ResourceMissingException(val clazz: Class<out Any>, val identifier: Any, val identifierType: String = "id") :
    RuntimeException(
        "Resource of type ${clazz.simpleName} with $identifierType $identifier is missing"
    )