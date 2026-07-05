package io.github.kperczynski.domain.user

import io.github.ktor_batterypack.core.exception.ErrorCode

enum class UserErrorCode(override val message: String) : ErrorCode {
    INVALID_USER_AGE("User age must be greater than 0, lower than 100, current is %s"), ;

    override val code: String
        get() = name
}