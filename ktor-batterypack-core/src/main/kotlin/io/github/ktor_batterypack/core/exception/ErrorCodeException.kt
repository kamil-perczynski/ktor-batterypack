package io.github.ktor_batterypack.core.exception

class ErrorCodeException(val errorCode: ErrorCode, vararg args: Any) :
    RuntimeException(errorCode.message.format(*args))
