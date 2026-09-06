package io.github.kperczynski.libs.ktor.client

import io.github.ktor_batterypack.validation.Constraints
import io.github.ktor_batterypack.validation.ValidationCall

object EmailConstraints {

    fun checkEmail(prop: String, str: String, call: ValidationCall) {
        Constraints.checkEmail(prop, str, call, regexp = ".*")
    }

}