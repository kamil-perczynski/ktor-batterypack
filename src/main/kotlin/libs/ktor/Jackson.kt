package io.github.kperczynski.libs.ktor

import io.ktor.http.ContentType
import io.ktor.serialization.jackson3.JacksonConverter
import io.ktor.server.plugins.contentnegotiation.*
import tools.jackson.databind.json.JsonMapper

fun ContentNegotiationConfig.jacksonSerialization(jsonMapper: JsonMapper) {
    register(ContentType.Application.Json, JacksonConverter(jsonMapper))
}
