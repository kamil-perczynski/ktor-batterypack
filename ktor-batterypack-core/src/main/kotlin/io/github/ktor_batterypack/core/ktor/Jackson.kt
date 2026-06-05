package io.github.ktor_batterypack.core.ktor

import io.ktor.http.ContentType
import io.ktor.serialization.jackson3.JacksonConverter
import io.ktor.server.plugins.contentnegotiation.*
import tools.jackson.databind.json.JsonMapper

/**
 * Registers a Jackson JSON converter for content negotiation.
 */
fun ContentNegotiationConfig.jacksonSerialization(jsonMapper: JsonMapper) {
    register(ContentType.Application.Json, JacksonConverter(jsonMapper))
}
