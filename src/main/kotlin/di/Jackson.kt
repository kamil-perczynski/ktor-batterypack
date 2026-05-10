package io.github.kperczynski.di

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.serialization.jackson.*
import io.ktor.server.plugins.contentnegotiation.*

/**
 * Configure Jackson serialization for ContentNegotiation.
 * This can be used as an alternative to kotlinx.serialization JSON.
 */
fun ContentNegotiationConfig.jacksonSerialization() {
    jackson {
        // Enable pretty printing for development
        enable(SerializationFeature.INDENT_OUTPUT)
        // Disable writing dates as timestamps - write them as ISO strings
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
}

