package io.github.ktor_batterypack.core.di

import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(KoinLifecycleListener::class.java)

/**
 * Koin-based lifecycle listener that runs initialization callbacks
 * on startup and closes resources on shutdown.
 */
@Singleton
class KoinLifecycleListener(
    private val closeCallbacks: List<AutoCloseable>,
    private val initCallbacks: List<InitCallback>,
    private val bootstrapCallbacks: List<BootstrapCallback>
) : LifecycleListener {

    /**
     * Runs all registered [BootstrapCallback] instances.
     */
    override fun onBootstrap() {
        log.info("Running {} bootstrap callbacks", bootstrapCallbacks.size)
        for (callback in bootstrapCallbacks) {
            callback.onBootstrap()
        }
    }

    /**
     * Runs all registered [InitCallback] instances.
     */
    override fun onStart() {
        log.info("Application has started. Running {} initialization callbacks...", initCallbacks.size)

        for (callback in initCallbacks) {
            try {
                callback.onInit()
            } catch (e: Exception) {
                log.error("Initialization callback failed", e)
                throw IllegalStateException("Initialization callback=${callback.javaClass.name} failed", e)
            }
        }
    }

    /**
     * Closes all registered [AutoCloseable] resources.
     */
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
