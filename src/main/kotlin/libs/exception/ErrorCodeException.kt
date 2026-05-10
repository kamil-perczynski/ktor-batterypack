package io.github.kperczynski.libs.exception

class ErrorCodeException(val errorCode: ErrorCode, vararg args: Any) :
    RuntimeException(errorCode.message.format(*args))