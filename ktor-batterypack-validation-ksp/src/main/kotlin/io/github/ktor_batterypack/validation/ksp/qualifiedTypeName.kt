package io.github.ktor_batterypack.validation.ksp

fun qualifiedTypeName(type: CodegenType): String {
    if (type.typeParams.isEmpty()) {
        return type.fqName
    }
    val typeArgs = type.typeParams.joinToString(", ") { typeParam ->
        if (typeParam.isMarkedNullable) qualifiedTypeName(typeParam) + '?' else qualifiedTypeName(typeParam)
    }
    return "${type.fqName}<$typeArgs>"
}
