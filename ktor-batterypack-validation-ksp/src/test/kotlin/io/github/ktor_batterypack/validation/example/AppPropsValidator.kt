package io.github.ktor_batterypack.validation.example

import io.github.ktor_batterypack.validation.ValidationResult

@Suppress("unused")
interface AppPropsValidator {

    fun validateAppProps(appProps: AppProps): ValidationResult<AppProps>

}