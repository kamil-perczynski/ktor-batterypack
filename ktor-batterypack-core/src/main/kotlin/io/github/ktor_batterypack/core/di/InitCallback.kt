package io.github.ktor_batterypack.core.di

/**
 * Callback interface invoked during application initialization.
 */
fun interface InitCallback {
    /**
     * Performs initialization logic.
     */
    fun onInit()
}
