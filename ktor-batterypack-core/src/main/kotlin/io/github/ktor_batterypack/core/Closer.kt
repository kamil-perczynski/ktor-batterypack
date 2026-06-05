package io.github.ktor_batterypack.core

/**
 * Collects deferred cleanup callbacks and runs them in reverse order on [close].
 */
class Closer {

    private val callbacks = mutableListOf<() -> Unit>()

    /**
     * Registers a callback to be executed when this closer is closed.
     */
    fun add(callback: () -> Unit) {
        callbacks.add(callback)
    }

    /**
     * Executes all registered callbacks in reverse registration order, catching and logging failures.
     */
    fun close() {
        callbacks.reversed().forEach {
            runCatching(it).onFailure { ex ->
                System.err.println("Closer callback failed: ${ex.message}")
            }
        }
        callbacks.clear()
    }
}
