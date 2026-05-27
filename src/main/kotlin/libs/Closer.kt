package io.github.kperczynski.libs

/**
 * Utility class to manage multiple closeable resources.
 * It allows adding callbacks that will be executed when the `close` method is called.
 */
class Closer {

    private val callbacks = mutableListOf<() -> Unit>()

    fun add(callback: () -> Unit) {
        callbacks.add(callback)
    }

    fun close() {
        callbacks.reversed().forEach {
            runCatching(it).onFailure { ex ->
                System.err.println("Closer callback failed: ${ex.message}")
            }
        }
        callbacks.clear()
    }
}