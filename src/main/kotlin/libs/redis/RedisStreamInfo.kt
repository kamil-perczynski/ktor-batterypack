package io.github.kperczynski.libs.redis

fun parseXInfoStreamLastGeneratedId(raw: List<*>): String? {
    val map = rawToMap(raw)
    return map["last-generated-id"] as? String
}

private fun rawToMap(raw: List<*>): Map<String, Any?> {
    val result = mutableMapOf<String, Any?>()
    var i = 0
    while (i < raw.size - 1) {
        val key = raw[i] as? String ?: run { i += 2; continue }
        result[key] = raw[i + 1]
        i += 2
    }
    return result
}
