package io.github.ktor_batterypack.core.ktor

import io.github.ktor_batterypack.core.exception.ErrorCodeException
import io.github.ktor_batterypack.core.exception.ResourceMissingException
import io.github.ktor_batterypack.core.problemdetail.ProblemDetail
import io.github.ktor_batterypack.validation.ValidationException
import io.ktor.http.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory
import tools.jackson.databind.json.JsonMapper

private val log = LoggerFactory.getLogger(KtorExceptionHandler::class.java)

/**
 * Global exception handler that maps common exceptions to RFC 7807 [ProblemDetail] responses.
 *
 * @property jsonMapper Jackson mapper used for serializing error details.
 */
@Singleton
class KtorExceptionHandler(private val jsonMapper: JsonMapper) {

    /**
     * Registers all exception handlers on the given [StatusPagesConfig].
     */
    fun register(it: StatusPagesConfig) {
        registerResourceMissing(it)
        registerErrorCodeException(it)
        registerIllegalArgument(it)
        registerValidationException(it)
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

    private fun registerValidationException(it: StatusPagesConfig) {
        it.exception<ValidationException> { call, cause ->
            val problemDetail = ProblemDetail(
                type = "VALIDATION_ERROR",
                title = "Validation Failed",
                status = 400,
                detail = "Request validation failed",
                instance = call.request.uri,
                extensionData = mapOf("validation" to cause.errors)
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
