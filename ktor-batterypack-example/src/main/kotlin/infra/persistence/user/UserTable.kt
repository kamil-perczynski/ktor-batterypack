package io.github.kperczynski.infra.persistence.user

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UIntIdTable
import org.jetbrains.exposed.v1.dao.UIntEntity
import org.jetbrains.exposed.v1.dao.UIntEntityClass

object UserTable : UIntIdTable(name = "users") {
    val name = varchar("name", length = 50)
    val age = integer("age")
}

class UserEntity(id: EntityID<UInt>) : UIntEntity(id) {
    companion object : UIntEntityClass<UserEntity>(UserTable)

    var name by UserTable.name
    var age by UserTable.age
}
