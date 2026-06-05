package io.github.kperczynski.infra

import io.github.kperczynski.infra.client.FlorinClientProps
import io.github.kperczynski.libs.db.DatabaseProps
import io.github.ktor_batterypack.redis.RedisProps
import io.github.ktor_batterypack.core.ktor.KtorProps

data class AppProps(
    val ktor: KtorProps = KtorProps(),
    val database: DatabaseProps = DatabaseProps(),
    val florin: FlorinClientProps = FlorinClientProps(),
    val redis: RedisProps = RedisProps()
)
