package io.github.kperczynski.controllers

import io.github.kperczynski.infra.AppProps
import io.github.ktor_batterypack.annotation.Validator
import io.github.ktor_batterypack.validation.ValidationResult

@Validator
interface AppPropsValidator {

    companion object {
        val appPropsValidator = AppPropsValidatorImpl()
    }

    fun validate(props: AppProps): ValidationResult<AppProps>

}
