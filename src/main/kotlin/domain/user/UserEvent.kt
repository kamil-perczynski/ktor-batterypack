package io.github.kperczynski.domain.user

data class UserEvent(
    val userId: String,
    val type: UserEventType,
    val meta: Map<String, String> = emptyMap()
)

enum class UserEventType {
    USER_READ,
    USER_CREATED,
    USER_UPDATED,
}