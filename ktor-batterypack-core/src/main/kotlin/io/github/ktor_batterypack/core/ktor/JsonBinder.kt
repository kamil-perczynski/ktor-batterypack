package io.github.ktor_batterypack.core.ktor

import io.github.ktor_batterypack.validation.ValidationResult
import io.github.ktor_batterypack.validation.check
import io.ktor.server.application.*
import io.ktor.server.request.*
import org.koin.core.annotation.Singleton
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.convertValue

/**
 * Binds Ktor request inputs (JSON body, query parameters) to typed objects, validating
 * them before conversion.
 *
 * The input is first materialized as a Jackson [JsonNode], passed through the supplied
 * validator, and the validated node is then converted to the target type with
 * [jsonMapper].
 */
@Singleton
class JsonBinder(@property:PublishedApi internal val jsonMapper: JsonMapper) {

    /**
     * Receives the request body of [call] as a [JsonNode], validates it with
     * [validatorFn] and converts the validated node to [T].
     *
     * ```kotlin
     * routing.post("/users") {
     *     val userCreate = jsonBinder.bindBody<UserCreateDto>(call, userApiValidator::checkUserCreateDto)
     *     call.respond(HttpStatusCode.Created, userService.create(userCreate))
     * }
     * ```
     * @param call current application call
     * @param validatorFn validation function applied to the received body
     * @return request body converted to [T]
     * @throws io.github.ktor_batterypack.validation.ValidationException if [validatorFn]
     * reports the body as invalid
     */
    suspend inline fun <reified T> bindBody(
        call: ApplicationCall,
        validatorFn: (JsonNode) -> ValidationResult<JsonNode>
    ): T {
        val json = call.receive<JsonNode>()
        val validJson = validatorFn(json).check("Invalid request body")
        return jsonMapper.convertValue<T>(validJson)
    }

    /**
     * Converts the query parameters of [call] into a nested [JsonNode] via [QsBinder],
     * validates it with [validatorFn] and converts the validated node to [T].
     *
     * ```kotlin
     * routing.get("/plants") {
     *     val query = binder.bindQueryParams<ListPlantsParamsDto>(call, plantsApiValidator::checkListPlantsParamsDto)
     *     call.respond(HttpStatusCode.OK, listPlants(query))
     * }
     * ```
     *
     * @param call current application call
     * @param validatorFn validation function applied to the converted query parameters
     * @return query parameters converted to [T]
     * @throws io.github.ktor_batterypack.validation.ValidationException if [validatorFn]
     * reports the parameters as invalid
     */
    inline fun <reified T> bindQueryParams(
        call: ApplicationCall,
        validatorFn: (JsonNode) -> ValidationResult<JsonNode>
    ): T {
        val json = QsBinder.convert(call.request.queryParameters)
        val validJson = validatorFn(json).check("Invalid query string parameters")
        return jsonMapper.convertValue<T>(validJson)
    }

}

