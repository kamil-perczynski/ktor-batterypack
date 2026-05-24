package io.github.kperczynski.libs.redis.bgloops

/**
 * Data class representing information about a Redis consumer.
 *
 * @property name The name of the consumer.
 * @property pending The number of pending messages for this consumer.
 * @property idle The idle time in milliseconds since the last message was delivered to this consumer.
 */
data class RedisConsumerInfo(
    val name: String,
    val pending: Long,
    val idle: Long,
    val inactive: Long? = null
)

fun toXInfoResultDto(raw: List<*>): List<RedisConsumerInfo> {
    return raw.mapNotNull { entry ->
        val map = entry as? List<*> ?: return@mapNotNull null
        val consumerId = map.getOrNull(1) as? String? ?: return@mapNotNull null
        val pending = (map.getOrNull(3) as? Number)?.toLong() ?: return@mapNotNull null
        val idle = (map.getOrNull(5) as? Number)?.toLong() ?: return@mapNotNull null
        val inactive = (map.getOrNull(7) as? Number)?.toLong()
        RedisConsumerInfo(consumerId, pending, idle, inactive)
    }
}
