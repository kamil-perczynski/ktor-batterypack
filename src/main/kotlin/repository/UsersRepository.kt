package io.github.kperczynski.repository

import io.github.kperczynski.di.InitCallback
import io.github.kperczynski.exception.ResourceMissingException
import org.jetbrains.exposed.v1.core.dao.id.UIntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory

object ExposedUsers : UIntIdTable(name = "users") {
    val name = varchar("name", length = 50)
    val age = integer("age")
}

private val log = LoggerFactory.getLogger(UsersRepository::class.java)

@Singleton(createdAtStart = true)
class UsersRepository(private val database: Database) : InitCallback {


    override fun onInit() {
        transaction(database) {
            log.info("Creating 'Users' table if it does not exist...")
            SchemaUtils.create(ExposedUsers)
        }
    }

    fun create(user: User): UInt = transaction(database) {
        val newRecord = ExposedUsers.insert {
            it[name] = user.name
            it[age] = user.age
        }
        newRecord[ExposedUsers.id].value
    }

    fun read(id: UInt): User {
        return transaction(database) {
            val user = ExposedUsers.selectAll()
                .where { ExposedUsers.id eq id }
                .map { User(it[ExposedUsers.name], it[ExposedUsers.age]) }
                .singleOrNull()
                ?: throw ResourceMissingException(User::class.java, id)

            user
        }
    }

    fun update(id: UInt, user: User) {
        transaction(database) {
            ExposedUsers.update({ ExposedUsers.id eq id }) {
                it[name] = user.name
                it[age] = user.age
            }
        }
    }

    fun delete(id: UInt) {
        transaction(database) {
            ExposedUsers.deleteWhere { ExposedUsers.id.eq(id) }
        }
    }

}
