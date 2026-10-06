package io.github.kperczynski.infra.oidc

class IdpHttpClientException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
