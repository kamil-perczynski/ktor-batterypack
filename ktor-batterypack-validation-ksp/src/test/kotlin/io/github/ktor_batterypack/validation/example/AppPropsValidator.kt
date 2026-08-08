package io.github.ktor_batterypack.validation.example

import io.github.ktor_batterypack.validation.ValidationResult
import io.github.ktor_batterypack.validation.model.Person

interface AppPropsValidator {

    fun validateAppProps(appProps: AppProps): ValidationResult<AppProps>

    fun validatePerson(person: Person): ValidationResult<Person>

}