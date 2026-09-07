package io.github.kperczynski.controllers

import io.github.kperczynski.infra.ConfigMap
import io.github.ktor_batterypack.annotation.Validator
import io.github.ktor_batterypack.validation.ValidationResult

@Validator
interface ConfigMapValidator {

    companion object : ConfigMapValidator by ConfigMapValidatorImpl()

    fun validate(props: ConfigMap): ValidationResult<ConfigMap>

}
