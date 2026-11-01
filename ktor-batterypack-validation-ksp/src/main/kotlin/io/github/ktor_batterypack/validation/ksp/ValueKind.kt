package io.github.ktor_batterypack.validation.ksp

enum class ValueKind {
    SCALAR,
    ENUM,
    OBJECT,
    LIST,
    MAP;

    companion object {
        fun of(type: CodegenType): ValueKind {
            return when {
                type.isMap -> MAP
                type.isCollection -> LIST
                type.isEnum -> ENUM
                type.isPrimitive -> SCALAR
                else -> OBJECT
            }
        }
    }
}
