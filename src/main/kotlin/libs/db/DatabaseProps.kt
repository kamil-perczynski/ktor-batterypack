package io.github.kperczynski.libs.db

data class DatabaseProps(
    val url: String = "jdbc:postgresql://localhost:5432/ktordb",
    val user: String = "ktor",
    val password: String = "ktorpassword",
    val driver: String = "org.postgresql.Driver",
    val poolSize: Int = 2
)