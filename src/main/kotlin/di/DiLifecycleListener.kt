package io.github.kperczynski.di

import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(DiLifecycleListener::class.java)

@Singleton(createdAtStart = true)
class DiLifecycleListener(
    private val autoCloseables: List<AutoCloseable>,
    private val initCallbacks: List<InitCallback>
) : LifecycleListener {

    override fun onStop() {
        log.info("Application is stopping. Closing resources...")

        for (autoCloseable in autoCloseables) {
            try {
                autoCloseable.close()
            } catch (e: Exception) {
                // Log the error but continue closing other resources
                println("Error closing resource: $autoCloseable - ${e.message}")
            }
        }
    }

    override fun onStart() {
        log.info("Application has started. Running initialization callbacks...")

        for (callback in initCallbacks) {
            try {
                callback.onInit()
            } catch (e: Exception) {
                // Log the error but continue running other callbacks
                println("Error running init callback: $callback - ${e.message}")
            }
        }
    }
}

