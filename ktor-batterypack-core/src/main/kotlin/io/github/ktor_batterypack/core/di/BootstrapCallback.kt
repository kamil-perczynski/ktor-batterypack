package io.github.ktor_batterypack.core.di

/**
 * Callback interface invoked during application initialization.
 */
fun interface BootstrapCallback {
    /**
     * Performs initialization logic.
     */
    fun onBootstrap()
}
