package io.github.kperczynski.controllers

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

    suspend inline fun <reified T> bind(
        call: ApplicationCall,
        validatorFn: (JsonNode) -> ValidationResult<JsonNode>
    ): T {
        val json = call.receive<JsonNode>()
        val validJson = validatorFn(json).check()
        return jsonMapper.convertValue<T>(validJson)
    }

}