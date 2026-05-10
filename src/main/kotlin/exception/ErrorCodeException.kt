package io.github.kperczynski.exception

interface ErrorCodeException {
    val code: String
    val message: String
}