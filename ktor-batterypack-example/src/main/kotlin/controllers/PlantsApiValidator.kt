package io.github.kperczynski.controllers

import io.github.ktor_batterypack.annotation.JsonValidator
import io.github.ktor_batterypack.validation.ValidationParamType
import io.github.ktor_batterypack.validation.ValidationResult
import pl.kperczynski.florin.rest.dto.CareTaskSnoozeRequestDto
import pl.kperczynski.florin.rest.dto.ListPlantsParamsParameterDto
import pl.kperczynski.florin.rest.dto.PlantCreateDto
import tools.jackson.databind.JsonNode

@JsonValidator
interface PlantsApiValidator {

    companion object {
        val plantsApiValidator: PlantsApiValidator = PlantsApiValidatorImpl()
    }

    @ValidationParamType(ListPlantsParamsParameterDto::class)
    fun checkListPlantsParamsParameterDto(dto: JsonNode): ValidationResult<JsonNode>

    @ValidationParamType(CareTaskSnoozeRequestDto::class)
    fun validateCareTaskSnoozeRequestDto(dto: JsonNode): ValidationResult<JsonNode>

    @ValidationParamType(PlantCreateDto::class)
    fun checkPlantCreateDto(dto: JsonNode): ValidationResult<JsonNode>

}