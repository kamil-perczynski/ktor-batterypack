package io.github.kperczynski.controllers

import io.github.ktor_batterypack.annotation.Validator
import io.github.ktor_batterypack.core.ktor.KtorProps
import io.github.ktor_batterypack.validation.ValidationResult

@Validator
interface AppPropsValidator {

    fun validate(props: KtorProps): ValidationResult<KtorProps>

}