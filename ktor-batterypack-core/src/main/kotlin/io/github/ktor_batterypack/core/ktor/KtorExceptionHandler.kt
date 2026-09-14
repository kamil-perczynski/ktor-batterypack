package io.github.ktor_batterypack.core.ktor

import io.ktor.server.plugins.statuspages.StatusPagesConfig

/**
 * Registers exception-to-[io.github.ktor_batterypack.core.problemdetail.ProblemDetail] mappings on a [StatusPagesConfig].
 */
interface KtorExceptionHandler {

    fun register(it: StatusPagesConfig)

}
