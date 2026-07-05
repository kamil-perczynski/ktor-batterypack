package io.github.kperczynski.domain.user

import kotlinx.serialization.Serializable

@Serializable
data class User(val id : UInt, val name: String, val age: Int)
