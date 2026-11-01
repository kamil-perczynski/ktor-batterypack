package io.github.ktor_batterypack.validation.ksp

private val PLATFORM_PACKAGE_PREFIXES = listOf("java.", "javax.", "jakarta.", "kotlin.", "kotlinx.")

fun isPrimitive(itemClass: String): Boolean {
    return PLATFORM_PACKAGE_PREFIXES.any { prefix -> itemClass.startsWith(prefix) }
}
