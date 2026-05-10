package io.github.kperczynski

import io.github.kperczynski.libs.ktor.KtorController
import io.github.kperczynski.libs.exception.ErrorCodeException
import io.github.kperczynski.libs.exception.ResourceMissingException
import io.github.kperczynski.infra.KtorFrameApp
import io.github.kperczynski.libs.di.LifecycleListener
import io.github.kperczynski.libs.ktor.jacksonSerialization
import io.github.kperczynski.libs.problemdetail.ProblemDetail
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.Koin
import org.koin.ktor.plugin.KoinApplicationStarted
import org.koin.ktor.plugin.KoinApplicationStopPreparing
import org.koin.ktor.plugin.koin
import org.koin.plugin.module.dsl.withConfiguration

fun Application.configureServer() {
    monitor.subscribe(KoinApplicationStarted) {
        log.debug("Application has started. Notifying lifecycle listener")
        val lifecycleListener: LifecycleListener = get()
        lifecycleListener.onStart()
    }

    monitor.subscribe(KoinApplicationStopPreparing) {
        log.debug("Application is stopping. Notifying lifecycle listener")
        val lifecycleListener: LifecycleListener = get()
        lifecycleListener.onStop()
    }

    install(Koin) {
        withConfiguration<KtorFrameApp>()
        createEagerInstances()
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            val problemDetail = ProblemDetail(
                type = "about:blank",
                title = "Internal Server Error",
                status = 500,
                detail = cause.message ?: "An unexpected error occurred",
                instance = call.request.uri
            )
            call.respond(HttpStatusCode.InternalServerError, problemDetail)
        }

        exception<ResourceMissingException> { call, cause ->
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
        exception<ErrorCodeException> { call, cause ->
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

        status(HttpStatusCode.NotFound) { call, status ->
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

    install(ContentNegotiation) {
        jacksonSerialization()
    }

    val koin = koin()

    val controllers = koin.getAll<KtorController>()

    routing {
        for (controller in controllers) {
            controller.register(this)
        }
    }
}
