package io.github.kperczynski.libs.redis

data class RedisStreamGroupInfo(
    val name: String,
    val pending: Long,
    val lastDeliveredId: String,
)

fun toXInfoGroupResultDto(raw: List<*>): List<RedisStreamGroupInfo> {
    return raw.mapNotNull { entry ->
        val map = entry as? Map<*, *> ?: return@mapNotNull null
        val name = map["name"] as? String ?: return@mapNotNull null
        val pending = (map["pending"] as? Number)?.toLong() ?: return@mapNotNull null
        val lastDeliveredId = map["last-delivered-id"] as? String ?: return@mapNotNull null
        RedisStreamGroupInfo(name, pending, lastDeliveredId)
    }
}
