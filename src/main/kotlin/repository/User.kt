package io.github.kperczynski.repository

import kotlinx.serialization.Serializable

@Serializable
data class User(val name: String, val age: Int)
