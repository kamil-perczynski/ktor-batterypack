package io.github.kperczynski.infra.oidc

import io.github.ktor_batterypack.core.ktor.KtorExceptionHandler
import io.github.ktor_batterypack.core.problemdetail.ProblemDetail
import io.ktor.http.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.koin.core.annotation.Singleton

@Singleton
class IdpHttpClientExceptionHandler : KtorExceptionHandler {

    override fun register(it: StatusPagesConfig) {
        it.exception<IdpHttpClientException> { call, cause ->
            call.respond(
                HttpStatusCode.Unauthorized,
                ProblemDetail(
                    type = "ACCESS_TOKEN_INVALID",
                    title = "Unauthorized",
                    status = 401,
                    detail = cause.message,
                    instance = call.request.uri
                )
            )
        }
    }
}
