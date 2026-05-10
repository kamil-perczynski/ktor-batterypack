package io.github.kperczynski.libs.di

import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(KoinLifecycleListener::class.java)

class KoinLifecycleListener(
    private val closeCallbacks: List<AutoCloseable>,
    private val initCallbacks: List<InitCallback>
) : LifecycleListener {

    override fun onStart() {
        log.info("Application has started. Running initialization callbacks...")

        for (callback in initCallbacks) {
            try {
                callback.onInit()
            } catch (e: Exception) {
                log.error("Initialization callback failed", e)
            }
        }
    }

    override fun onStop() {
        log.info("Application is stopping. Closing resources...")

        for (closeCallback in closeCallbacks) {
            try {
                closeCallback.close()
            } catch (e: Exception) {
                log.error("Failed to close resource", e)
            }
        }
    }
}
