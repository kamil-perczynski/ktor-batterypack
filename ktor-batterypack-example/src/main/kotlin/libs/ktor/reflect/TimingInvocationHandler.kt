package io.github.kperczynski.libs.ktor.reflect

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED

/**
 * Dynamic proxy [InvocationHandler] that records Micrometer `repo.operation` timers
 * for repository calls, including `suspend` methods.
 */
class TimingInvocationHandler(
    private val delegate: Any,
    private val interfaceName: String,
    private val meterRegistry: MeterRegistry
) : InvocationHandler {

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        if (method.name == "equals" || method.name == "hashCode" || method.name == "toString") {
            return if (args != null) method.invoke(delegate, *args) else method.invoke(delegate)
        }

        // Drop Kotlin compiler-generated suffixes for suspend functions
        val methodName = method.name.substringBefore('-').substringBefore('$')
        val methodId = "${interfaceName}.$methodName"
        val sample = Timer.start(meterRegistry)

        if (isSuspendFunction(method)) {
            return invokeSuspend(method, args!!, methodId, sample)
        }

        try {
            val result =
                if (args != null) method.invoke(delegate, *args) else method.invoke(delegate)
            sample.stop(createRepoOperationTimer(methodId, "n/a"))
            return result
        } catch (ex: Throwable) {
            stopOnFailure(sample, methodId, ex)
            throw ex
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun invokeSuspend(
        method: Method,
        args: Array<out Any>,
        methodId: String,
        sample: Timer.Sample
    ): Any? {
        val original = args.last() as Continuation<Any?>
        val timed = object : Continuation<Any?> {
            override val context: CoroutineContext
                get() = original.context

            override fun resumeWith(result: Result<Any?>) {
                val throwable = result.exceptionOrNull()?.let { it::class.java.simpleName } ?: "n/a"
                sample.stop(createRepoOperationTimer(methodId, throwable))
                original.resumeWith(result)
            }
        }

        val timedArgs = (args as Array<Any?>).copyOf()
        timedArgs[timedArgs.size - 1] = timed

        return try {
            val result = method.invoke(delegate, *timedArgs)
            if (result !== COROUTINE_SUSPENDED) {
                sample.stop(createRepoOperationTimer(methodId, "n/a"))
            }
            result
        } catch (ex: Throwable) {
            stopOnFailure(sample, methodId, ex)
            throw ex
        }
    }

    private fun stopOnFailure(sample: Timer.Sample, methodId: String, ex: Throwable) {
        val throwable = (ex as? InvocationTargetException)?.targetException ?: ex
        sample.stop(createRepoOperationTimer(methodId, throwable::class.java.simpleName))
    }

    private fun createRepoOperationTimer(methodId: String, throwable: String): Timer {
        return Timer.builder("repo.operation")
            .tags(
                "methodId",
                methodId,
                "throwable",
                throwable
            )
            .publishPercentiles(0.5, 0.9, 0.95, 0.99)
            .register(meterRegistry)
    }

}

private fun isSuspendFunction(method: Method): Boolean {
    return method.parameterTypes.isNotEmpty() && method.parameterTypes.last() == Continuation::class.java
}
