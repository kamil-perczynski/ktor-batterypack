package io.github.ktor_batterypack.core.ktor

import io.github.ktor_batterypack.validation.ValidationResult
import io.github.ktor_batterypack.validation.check
import io.ktor.server.application.*
import io.ktor.server.request.*
import org.koin.core.annotation.Singleton
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.convertValue

@Singleton
class JsonBinder(@property:PublishedApi internal val jsonMapper: JsonMapper) {

    suspend inline fun <reified T> bindBody(
        call: ApplicationCall,
        validatorFn: (JsonNode) -> ValidationResult<JsonNode>
    ): T {
        val json = call.receive<JsonNode>()
        val validJson = validatorFn(json).check("Invalid request body")
        return jsonMapper.convertValue<T>(validJson)
    }

    inline fun <reified T> bindQueryParams(
        call: ApplicationCall,
        validatorFn: (JsonNode) -> ValidationResult<JsonNode>
    ): T {
        val json = QsBinder.convert(call.request.queryParameters)
        val validJson = validatorFn(json).check("Invalid query string parameters")
        return jsonMapper.convertValue<T>(validJson)
    }

}

