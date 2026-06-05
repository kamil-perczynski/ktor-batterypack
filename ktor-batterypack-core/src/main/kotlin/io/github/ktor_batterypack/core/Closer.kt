package io.github.ktor_batterypack.core

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
