package io.github.kperczynski.libs.ktor.reflect

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED

/**
 * Dynamic proxy [InvocationHandler] that records Micrometer `repo.operation` timers
 * for repository calls, including `suspend` methods.
 *
 * Method metadata and Micrometer [Timer] instances are cached per method so that the
 * hot path avoids string allocations, defensive reflection array copies, and meter
 * registry lookups on every invocation.
 */
class TimingInvocationHandler(
    private val delegate: Any,
    private val interfaceName: String,
    private val meterRegistry: MeterRegistry
) : InvocationHandler {

    private data class MethodMetadata(
        val methodId: String,
        val isSuspend: Boolean
    )

    private val methodMetadata = ConcurrentHashMap<Method, MethodMetadata>()
    private val timers = ConcurrentHashMap<String, ConcurrentHashMap<String, Timer>>()

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        if (method.isObjectMethod()) {
            return handleObjectMethod(method, args)
        }

        val metadata = methodMetadata.computeIfAbsent(method, ::computeMetadata)
        val sample = Timer.start(meterRegistry)

        if (metadata.isSuspend) {
            return invokeSuspend(method, args ?: emptyArray(), metadata.methodId, sample)
        }

        return try {
            val result = if (args != null) method.invoke(delegate, *args) else method.invoke(delegate)
            sample.stop(timer(metadata.methodId, "n/a"))
            result
        } catch (ex: Throwable) {
            stopOnFailure(sample, metadata.methodId, ex)
            throw ex
        }
    }

    private fun computeMetadata(method: Method): MethodMetadata {
        val methodName = method.name.substringBefore('-').substringBefore('$')
        val methodId = "$interfaceName.$methodName"
        val isSuspend = method.parameterCount > 0 && method.parameterTypes.last() == Continuation::class.java
        return MethodMetadata(methodId, isSuspend)
    }

    private fun Method.isObjectMethod(): Boolean = when (name) {
        "equals" -> parameterCount == 1 && parameterTypes[0] == Any::class.java
        "hashCode" -> parameterCount == 0
        "toString" -> parameterCount == 0
        else -> false
    }

    private fun handleObjectMethod(method: Method, args: Array<out Any>?): Any? = when (method.name) {
        "equals" -> delegate == args?.get(0)
        "hashCode" -> delegate.hashCode()
        "toString" -> delegate.toString()
        else -> throw AssertionError("Unexpected object method: ${method.name}")
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
                sample.stop(timer(methodId, throwable))
                original.resumeWith(result)
            }
        }

        val timedArgs = (args as Array<Any?>).copyOf()
        timedArgs[timedArgs.size - 1] = timed

        return try {
            val result = method.invoke(delegate, *timedArgs)
            if (result !== COROUTINE_SUSPENDED) {
                sample.stop(timer(methodId, "n/a"))
            }
            result
        } catch (ex: Throwable) {
            stopOnFailure(sample, methodId, ex)
            throw ex
        }
    }

    private fun stopOnFailure(sample: Timer.Sample, methodId: String, ex: Throwable) {
        val throwable = (ex as? InvocationTargetException)?.targetException ?: ex
        sample.stop(timer(methodId, throwable::class.java.simpleName))
    }

    private fun timer(methodId: String, throwable: String): Timer {
        return timers.computeIfAbsent(methodId) { ConcurrentHashMap() }
            .computeIfAbsent(throwable) { createRepoOperationTimer(methodId, throwable) }
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
