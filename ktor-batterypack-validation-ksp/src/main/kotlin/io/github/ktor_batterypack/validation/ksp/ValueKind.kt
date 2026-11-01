package io.github.ktor_batterypack.validation.ksp

enum class ValueKind {
    SCALAR,
    ENUM,
    OBJECT,
    LIST,
    MAP;

    companion object {
        private val PLATFORM_PACKAGE_PREFIXES = listOf("java.", "javax.", "jakarta.", "kotlin.", "kotlinx.")

        fun of(type: CodegenType): ValueKind {
            return when {
                type.isMap -> MAP
                type.isCollection -> LIST
                type.isEnum -> ENUM
                !type.isResolved || PLATFORM_PACKAGE_PREFIXES.any { prefix -> type.fqName.startsWith(prefix) } -> SCALAR
                else -> OBJECT
            }
        }
    }
}
