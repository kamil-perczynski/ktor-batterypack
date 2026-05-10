package io.github.kperczynski.domain.user

import io.github.kperczynski.libs.exception.ErrorCode

enum class UserErrorCode(override val message: String) : ErrorCode {
    USER_AGE_ILLEGAL("User age must be greater than 0, lower than 100, current is %s"), ;

    override val code: String
        get() = name
}