package io.github.kperczynski

import io.github.kperczynski.libs.ktor.reflect.TimingInvocationHandlerFactory
import io.micrometer.core.annotation.Timed
import io.micrometer.core.instrument.MeterRegistry
import org.koin.core.KoinApplication
import org.koin.core.annotation.KoinInternalApi
import org.koin.dsl.bind
import org.koin.dsl.module
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

/**
 * Wraps every registered bean whose primary interface name ends with "Repo"
 * in a dynamic proxy that records invocation timing via
 * [TimingInvocationHandlerFactory]. The proxy is bound to the same
 * interface/qualifier pair, overriding the original repository bean.
 *
 * This is intended for observability in regular workloads;
 * avoid for performance-critical paths because each call goes through
 * reflection-based proxy dispatch.
 */
@OptIn(KoinInternalApi::class)
fun enableTimedRepositories(koinApp: KoinApplication) {
    val koin = koinApp.koin

    val overrideModule = module {
        single<TimingInvocationHandlerFactory> {
            TimingInvocationHandlerFactory(get<MeterRegistry>())
        }

        koin.instanceRegistry.instances.forEach { (_, factory) ->
            val def = factory.beanDefinition

            if (!def.primaryType.annotations.any { it.annotationClass.qualifiedName == Timed::class.java.canonicalName }) {
                return@forEach
            }

            val ifClass = def.primaryType.java.interfaces.firstOrNull()
                ?: throw IllegalArgumentException("Interface is missing on ${def.primaryType.qualifiedName}")

            @Suppress("UNCHECKED_CAST")
            val klass = ifClass.kotlin as KClass<Any>

            single(qualifier = def.qualifier) {
                val originalTarget =
                    koin.scopeRegistry.rootScope.get<Any>(def.primaryType, def.qualifier)
                val handlerFactory = get<TimingInvocationHandlerFactory>()

                val proxy = Proxy.newProxyInstance(
                    ifClass.classLoader,
                    arrayOf(ifClass),
                    handlerFactory.create(originalTarget, ifClass.simpleName)
                )

                proxy
            } bind (klass)
        }
    }

    koinApp.modules(overrideModule)
}