package io.github.kperczynski.libs.ktor

import io.github.kperczynski.libs.exception.ErrorCodeException
import io.github.kperczynski.libs.exception.ResourceMissingException
import io.github.kperczynski.libs.problemdetail.ProblemDetail
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.request.uri
import io.ktor.server.response.respond

class KtorExceptionHandler {

    fun register(it: StatusPagesConfig) {
        registerResourceMissing(it)
        registerErrorCodeException(it)
        registerDefaultNotFound(it)
        registerInternalServerError(it)
    }

    private fun registerDefaultNotFound(it: StatusPagesConfig) {
        it.status(HttpStatusCode.NotFound) { call, status ->
            val problemDetail = ProblemDetail(
                type = "about:blank",
                title = "Not Found",
                status = status.value,
                detail = "The requested static resource was not found",
                instance = call.request.uri
            )
            call.respond(status, problemDetail)
        }
    }

    private fun registerErrorCodeException(it: StatusPagesConfig) {
        it.exception<ErrorCodeException> { call, cause ->
            val problemDetail = ProblemDetail(
                type = "about:blank",
                title = cause.errorCode.code,
                status = 422,
                detail = cause.message,
                instance = call.request.uri,
                extensionData = mapOf()
            )
            call.respond(HttpStatusCode.UnprocessableEntity, problemDetail)
        }
    }

    private fun registerResourceMissing(it: StatusPagesConfig) {
        it.exception<ResourceMissingException> { call, cause ->
            val problemDetail = ProblemDetail(
                type = "about:blank",
                title = "Not Found",
                status = 404,
                detail = cause.message,
                instance = call.request.uri,
                extensionData = mapOf(
                    "entity_type" to cause.clazz.simpleName,
                    "identifier" to cause.identifier,
                    "identifier_type" to cause.identifierType
                )
            )
            call.respond(HttpStatusCode.NotFound, problemDetail)
        }
    }

    private fun registerInternalServerError(it: StatusPagesConfig) {
        it.exception<Throwable> { call, cause ->
            val problemDetail = ProblemDetail(
                type = "about:blank",
                title = "Internal Server Error",
                status = 500,
                detail = cause.message ?: "An unexpected error occurred",
                instance = call.request.uri
            )
            call.respond(HttpStatusCode.InternalServerError, problemDetail)
        }
    }
}