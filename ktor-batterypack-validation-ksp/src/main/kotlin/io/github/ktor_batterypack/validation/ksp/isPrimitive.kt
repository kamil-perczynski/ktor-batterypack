package io.github.ktor_batterypack.validation.ksp

fun isPrimitive(itemClass: String, isEnum: Boolean): Boolean {
    // TODO: remove is enum check from here
    return isEnum || itemClass.startsWith("java") || itemClass.startsWith("kotlin")
}
