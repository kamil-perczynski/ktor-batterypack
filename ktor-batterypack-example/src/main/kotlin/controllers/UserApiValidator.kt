package io.github.kperczynski.controllers

import io.github.kperczynski.domain.user.UserCreate
import io.github.kperczynski.domain.user.UserUpdate
import io.github.ktor_batterypack.annotation.JsonValidator
import io.github.ktor_batterypack.validation.ValidationParamType
import io.github.ktor_batterypack.validation.ValidationResult
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.JsonNodeFactory

@JsonValidator
interface UserApiValidator {

    companion object : UserApiValidator by UserApiValidatorImpl()

    @ValidationParamType(UserCreate::class)
    fun validateUserCreate(userCreate: JsonNode): ValidationResult<JsonNode>

    @ValidationParamType(UserUpdate::class)
    fun validateUserUpdate(userUpdate: JsonNode): ValidationResult<JsonNode>

}
