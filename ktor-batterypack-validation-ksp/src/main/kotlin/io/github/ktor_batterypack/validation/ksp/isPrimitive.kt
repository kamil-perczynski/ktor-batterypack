package io.github.ktor_batterypack.validation.ksp

fun isPrimitive(itemClass: String, isEnum: Boolean): Boolean {
    return isEnum || itemClass.startsWith("java") || itemClass.startsWith("kotlin")
}
