package io.github.ktor_batterypack.validation.ksp

fun isPrimitive(itemClass: String): Boolean {
    return itemClass.startsWith("java") || itemClass.startsWith("kotlin")
}
