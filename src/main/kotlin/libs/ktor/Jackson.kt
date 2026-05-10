package io.github.kperczynski.libs.ktor

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.plugins.contentnegotiation.*

fun ContentNegotiationConfig.jacksonSerialization(objectMapper: ObjectMapper) {
    register(ContentType.Application.Json, JacksonConverter(objectMapper))
}
