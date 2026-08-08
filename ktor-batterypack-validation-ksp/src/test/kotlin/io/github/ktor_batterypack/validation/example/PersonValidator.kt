package io.github.ktor_batterypack.validation.example

import io.github.ktor_batterypack.validation.ValidationResult
import io.github.ktor_batterypack.validation.model.Person

interface PersonValidator {

    fun validate(person: Person): ValidationResult<Person>

}