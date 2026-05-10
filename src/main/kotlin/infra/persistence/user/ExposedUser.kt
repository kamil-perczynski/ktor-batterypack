package io.github.kperczynski.infra.persistence.user

import org.jetbrains.exposed.v1.core.dao.id.UIntIdTable

object ExposedUser : UIntIdTable(name = "users") {
    val name = varchar("name", length = 50)
    val age = integer("age")
}
