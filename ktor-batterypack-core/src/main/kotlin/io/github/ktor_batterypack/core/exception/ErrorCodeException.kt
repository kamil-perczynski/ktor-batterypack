package io.github.ktor_batterypack.core.exception

/**
 * Runtime exception that wraps an [ErrorCode] and formats its message with optional arguments.
 */
class ErrorCodeException(val errorCode: ErrorCode, vararg args: Any) :
    RuntimeException(errorCode.message.format(*args))
