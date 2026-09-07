package io.github.ktor_batterypack.metrics.reflect

import io.micrometer.core.instrument.MeterRegistry
import java.lang.reflect.InvocationHandler

/**
 * Factory for creating [TimingInvocationHandler] proxies for repository beans.
 */
class TimingInvocationHandlerFactory(private val meterRegistry: MeterRegistry) {

    /**
     * Creates an invocation handler that times calls on [originalTarget] exposed as [interfaceName].
     */
    fun create(originalTarget: Any, interfaceName: String): InvocationHandler {
        return TimingInvocationHandler(originalTarget, interfaceName,  meterRegistry)
    }

}
