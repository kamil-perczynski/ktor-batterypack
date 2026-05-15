package io.github.kperczynski.libs.ktor

import io.github.kperczynski.libs.exception.ErrorCodeException
import io.github.kperczynski.libs.exception.ResourceMissingException
import io.github.kperczynski.libs.problemdetail.ProblemDetail
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(KtorExceptionHandler::class.java)

class KtorExceptionHandler {

    fun register(it: StatusPagesConfig) {
        registerResourceMissing(it)
        registerErrorCodeException(it)
        registerIllegalArgument(it)
        registerDefaultNotFound(it)
        registerInternalServerError(it)
        registerMethodNotAllowed(it)
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

    private fun registerMethodNotAllowed(it: StatusPagesConfig) {
        it.status(HttpStatusCode.MethodNotAllowed) { call, status ->
            val problemDetail = ProblemDetail(
                type = "about:blank",
                title = "Method Not Allowed",
                status = status.value,
                detail = "The HTTP method used is not allowed for this endpoint",
                instance = call.request.uri
            )
            call.respond(status, problemDetail)
        }
    }

    private fun registerErrorCodeException(it: StatusPagesConfig) {
        it.exception<ErrorCodeException> { call, cause ->
            val problemDetail = ProblemDetail(
                type = cause.errorCode.code,
                title = "Unprocessable Entity",
                status = 422,
                detail = cause.message,
                instance = call.request.uri,
                extensionData = mapOf()
            )
            call.respond(HttpStatusCode.UnprocessableEntity, problemDetail)
        }
    }

    private fun registerIllegalArgument(it: StatusPagesConfig) {
        it.exception<IllegalArgumentException> { call, cause ->
            val problemDetail = ProblemDetail(
                type = "BAD_REQUEST",
                title = "Bad Request",
                status = 400,
                detail = cause.message ?: "Invalid request",
                instance = call.request.uri
            )
            call.respond(HttpStatusCode.BadRequest, problemDetail)
        }
    }

    private fun registerResourceMissing(it: StatusPagesConfig) {
        it.exception<ResourceMissingException> { call, cause ->
            val problemDetail = ProblemDetail(
                type = ResourceMissingException.ERROR_CODE,
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
                type = "INTERNAL_SERVER_ERROR",
                title = "Internal Server Error",
                status = 500,
                detail = cause.message ?: "An unexpected error occurred",
                instance = call.request.uri
            )

            log.error(
                "Unhandled exception occurred while processing request to {}",
                call.request.uri,
                cause
            )

            call.respond(HttpStatusCode.InternalServerError, problemDetail)
        }
    }
}