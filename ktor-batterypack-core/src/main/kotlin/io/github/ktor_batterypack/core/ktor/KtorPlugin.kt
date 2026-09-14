package io.github.ktor_batterypack.core.ktor

import io.ktor.server.application.Application

/**
 * Thin wrapper for a reusable Ktor plugin registration, discovered by
 * [io.github.ktor_batterypack.core.configureKtorServer] and registered against the [Application].
 */
interface KtorPlugin {

    fun register(app: Application)

}
