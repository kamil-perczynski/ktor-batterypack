package io.github.kperczynski.infra.florin

import io.github.kperczynski.infra.ConfigMap
import io.github.kperczynski.infra.client.FlorinClientProps
import io.github.ktor_batterypack.core.ktor.client.KtorHttpClientFactory
import io.ktor.client.HttpClient
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Singleton

@Module
@Configuration
class FlorinModule {

    @Singleton
    fun florinClientProps(@Provided configMap: ConfigMap): FlorinClientProps {
        return configMap.florin
    }

    @Singleton
    @Named("florin")
    fun florinHttpClient(factory: KtorHttpClientFactory, props: FlorinClientProps): HttpClient {
        return factory.createHttpClient(
            baseUrl = props.baseUrl,
            connectTimeoutMs = props.connectTimeoutMs,
            readTimeoutMs = props.readTimeoutMs
        )
    }
}