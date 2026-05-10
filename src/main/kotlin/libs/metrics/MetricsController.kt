package io.github.kperczynski.libs.metrics

import io.github.kperczynski.libs.ktor.KtorController
import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

class MetricsController(private val registry: PrometheusMeterRegistry) : KtorController {

    override fun register(routing: Routing) {
        routing.get("/actuator/prometheus") {
            call.respondText(
                text = registry.scrape(),
                contentType = ContentType.Text.Plain
            )
        }
    }

}
