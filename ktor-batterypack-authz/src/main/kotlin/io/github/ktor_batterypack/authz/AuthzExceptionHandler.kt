package io.github.ktor_batterypack.authz

import io.github.ktor_batterypack.core.ktor.KtorExceptionHandler
import io.github.ktor_batterypack.core.problemdetail.ProblemDetail
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.request.uri
import io.ktor.server.response.respond

/**
 * Maps [PermissionDeniedException] to an HTTP 403 [ProblemDetail].
 */
class AuthzExceptionHandler : KtorExceptionHandler {

    override fun register(it: StatusPagesConfig) {
        it.exception<PermissionDeniedException> { call, cause ->
            val problemDetail = ProblemDetail(
                type = "PERMISSION_DENIED",
                title = "Forbidden",
                status = 403,
                detail = cause.message,
                instance = call.request.uri,
                extensionData = mapOf("permission" to cause.permission)
            )
            call.respond(HttpStatusCode.Forbidden, problemDetail)
        }
    }
}
