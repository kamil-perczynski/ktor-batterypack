package io.github.ktor_batterypack.authz

import io.github.ktor_batterypack.core.ktor.KtorExceptionHandler
import org.koin.core.annotation.Module
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Singleton

/**
 * RBAC wiring: [JwtAuthFactory], [PermissionChecker] and [AuthzExceptionHandler] built from [AuthzProps].
 * Skip this module to provide a different permission system on top of the same abstractions.
 */
@Module
class KtorBatterypackAuthzModule {

    @Singleton
    fun jwtAuthFactory(@Provided authzProps: AuthzProps): JwtAuthFactory {
        return JwtAuthFactory(authzProps.jwt)
    }

    @Singleton
    fun permissionChecker(@Provided authzProps: AuthzProps): PermissionChecker {
        return PermissionChecker(authzProps)
    }

    @Singleton(binds = [KtorExceptionHandler::class])
    fun authzExceptionHandler(): AuthzExceptionHandler {
        return AuthzExceptionHandler()
    }

}
