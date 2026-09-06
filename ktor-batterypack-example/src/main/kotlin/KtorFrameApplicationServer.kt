package io.github.kperczynski

import io.github.kperczynski.infra.KtorFrameApp
import io.github.kperczynski.libs.ktor.reflect.TimingInvocationHandler
import io.github.kperczynski.libs.ktor.reflect.TimingInvocationHandlerFactory
import io.github.ktor_batterypack.core.configureKtorServer
import io.ktor.server.application.*
import org.koin.core.KoinApplication
import org.koin.core.annotation.KoinInternalApi
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.withConfiguration
import org.slf4j.LoggerFactory
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

/**
 * Configures the example Ktor application, wiring the domain module and enabling
 * timed proxies for repository beans.
 */
fun Application.configureServer() {
    configureKtorServer { ktorApp, koinApp, profiles ->
        koinApp.modules(
            module {
                single { ktorApp }
            }
        )
        koinApp.withConfiguration<KtorFrameApp>()
        koinApp.properties(mapOf("app.profiles" to profiles))
        enableTimedRepositories(koinApp)
    }
}

private val log = LoggerFactory.getLogger(TimingInvocationHandler::class.java)

@OptIn(KoinInternalApi::class)
private fun enableTimedRepositories(koinApp: KoinApplication) {
    val koin = koinApp.koin

    val overrideModule = module {
        koin.instanceRegistry.instances.forEach { (_, factory) ->
            val def = factory.beanDefinition
            val ifClass = def.primaryType.java.interfaces.find { it.simpleName.endsWith("Repo") }

            if (ifClass == null) {
                return@forEach
            }

            @Suppress("UNCHECKED_CAST")
            val klass = ifClass.kotlin as KClass<Any>

            single(qualifier = def.qualifier) {
                val originalTarget =
                    koin.scopeRegistry.rootScope.get<Any>(def.primaryType, def.qualifier)
                val handlerFactory =
                    koin.get<TimingInvocationHandlerFactory>()

                val proxy = Proxy.newProxyInstance(
                    ifClass.classLoader,
                    arrayOf(ifClass),
                    handlerFactory.create(originalTarget, ifClass.simpleName)
                )

                log.warn("Proxying $ifClass")
                proxy
            } bind (klass)
        }
    }

    koinApp.modules(overrideModule)
}


