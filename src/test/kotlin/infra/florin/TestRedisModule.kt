package io.github.kperczynski.infra.florin

import io.github.kperczynski.libs.di.InitCallback
import io.github.kperczynski.libs.redis.RedisModule
import io.github.kperczynski.libs.redis.RedisStreamFetcher
import io.github.kperczynski.libs.redis.RedisStreamListener
import io.github.kperczynski.libs.redis.RedisStreamListenerGroups.Companion.TEST_GROUP
import io.lettuce.core.RedisClient
import kotlinx.coroutines.asCoroutineDispatcher
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton
import java.util.concurrent.ThreadPoolExecutor

@Configuration
@Module(includes = [RedisModule::class])
class TestRedisModule {

    @Singleton(binds = [InitCallback::class, AutoCloseable::class])
    @Named("testRedisFetcher")
    fun testRedisFetcher(
        redisClient: RedisClient,
        listeners: List<RedisStreamListener>,
        @Named("redisStreamsThreadPool") threadPool: ThreadPoolExecutor
    ): RedisStreamFetcher {
        val filteredListeners = listeners.filter { it.group() == TEST_GROUP }
        val streams = filteredListeners.map { it.stream() }.distinct()

        return RedisStreamFetcher(
            fetcherId = "Test-1",
            redisClient = redisClient,
            listeners = filteredListeners,
            streams = streams,
            consumerGroup = "test",
            dispatcher = threadPool.asCoroutineDispatcher(),
        )
    }

}
