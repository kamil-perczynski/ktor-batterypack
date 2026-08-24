package io.github.kperczynski.controllers

import io.github.kperczynski.domain.user.UserCreate
import io.github.kperczynski.domain.user.UserUpdate
import io.github.ktor_batterypack.annotation.Validator
import io.github.ktor_batterypack.validation.ValidationResult

@Validator
interface UserDtoValidator {

    companion object {
        val userDtoValidator : UserDtoValidator = UserDtoValidatorImpl()
    }

    fun validate(userCreate: UserCreate): ValidationResult<UserCreate>

    fun validate(userUpdate: UserUpdate): ValidationResult<UserUpdate>

}
