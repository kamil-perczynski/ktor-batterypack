package io.github.kperczynski.infra.persistence.user

import io.github.kperczynski.libs.exception.ResourceMissingException
import io.github.kperczynski.domain.user.User
import io.github.kperczynski.domain.user.UserRepo
import io.github.kperczynski.libs.di.InitCallback
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(ExposedUserRepo::class.java)

@Singleton
class ExposedUserRepo(private val database: Database) : UserRepo, InitCallback {

    override fun onInit() {
        transaction(database) {
            log.info("Creating 'Users' table if it does not exist...")
            SchemaUtils.create(ExposedUser)
        }
    }

    override suspend fun create(user: User): User {
        return suspendTransaction(database) {
            val newRecord = ExposedUser.insert {
                it[name] = user.name
                it[age] = user.age
            }
            val id = newRecord[ExposedUser.id].value

            user.copy(id = id)
        }
    }

    override suspend fun find(id: UInt): User {
        return suspendTransaction(database) {
            val user = ExposedUser.selectAll()
                .where { ExposedUser.id eq id }
                .map { toUser(it) }
                .singleOrNull()
                ?: throw ResourceMissingException(User::class.java, id)

            user
        }
    }

    override suspend fun update(user: User) {
        suspendTransaction(database) {
            ExposedUser.update({ ExposedUser.id eq user.id }) {
                it[name] = user.name
                it[age] = user.age
            }
        }
    }

    override suspend fun delete(id: UInt) {
        suspendTransaction(database) {
            ExposedUser.deleteWhere { ExposedUser.id.eq(id) }
        }
    }

}

private fun toUser(row: ResultRow): User {
    return User(
        id = row[ExposedUser.id].value,
        name = row[ExposedUser.name],
        age = row[ExposedUser.age]
    )
}
