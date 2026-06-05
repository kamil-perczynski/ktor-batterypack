package io.github.kperczynski.libs.health

import io.github.ktor_batterypack.core.ktor.KtorController
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get

class HealthController(private val readinessEndpoint: ReadinessEndpoint) : KtorController {

    override fun register(routing: Routing) {
        routing.get("/actuator/health/liveness") {
            call.respond(LivenessResponse())
        }

        routing.get("/actuator/health/readiness") {
            val response = readinessEndpoint.check()

            val statusCode = when (response.status) {
                HealthStatus.UP -> HttpStatusCode.OK
                else -> HttpStatusCode.ServiceUnavailable
            }

            call.respond(statusCode, response)
        }
    }
}
