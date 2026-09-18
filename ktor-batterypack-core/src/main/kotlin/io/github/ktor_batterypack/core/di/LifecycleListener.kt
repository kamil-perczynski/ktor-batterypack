package io.github.ktor_batterypack.core.di

/**
 * Listener for application lifecycle events.
 */
interface LifecycleListener {
    /**
     * Called during bootstrap, before the application starts.
     */
    fun onBootstrap()

    /**
     * Called when the application starts.
     */
    fun onStart()

    /**
     * Called when the application stops.
     */
    fun onStop()
}
