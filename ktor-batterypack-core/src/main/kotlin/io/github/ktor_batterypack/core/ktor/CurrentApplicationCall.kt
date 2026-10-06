package io.github.ktor_batterypack.core.ktor

import io.ktor.server.application.*
import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.CoroutineContext

class CurrentApplicationCall(
    val call: ApplicationCall
) : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<CurrentApplicationCall>

    override val key: CoroutineContext.Key<*>
        get() = Key
}

suspend fun currentRoutingCall(): ApplicationCall {
    val ctx = currentCoroutineContext()
    val call = (ctx[CurrentApplicationCall]?.call
        ?: error("No ApplicationCall associated with the current coroutine"))

    return call
}
