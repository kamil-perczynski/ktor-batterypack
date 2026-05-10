package io.github.kperczynski.infra.client

data class FlorinClientProps(
    val baseUrl: String = "https://florin-be.k8s.kperczynski.click",
    val readTimeoutMs: Long = 15000L,
    val connectTimeoutMs: Long = 1000L
)
