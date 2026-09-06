package io.github.kperczynski.infra.persistence.user

import io.github.kperczynski.domain.user.User
import io.github.kperczynski.domain.user.UserRepo
import io.github.ktor_batterypack.core.di.InitCallback
import io.github.ktor_batterypack.core.exception.ResourceMissingException
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(ExposedUserRepo::class.java)

@Singleton
class ExposedUserRepo(private val database: Database) : UserRepo, InitCallback {

    override fun onInit() {
        transaction(database) {
            log.info("Creating 'Users' table if it does not exist...")
            SchemaUtils.create(UserTable)
        }
    }

    override suspend fun create(user: User): User {
        return suspendTransaction(database) {
            val entity = UserEntity.new {
                name = user.name
                age = user.age
            }
            toUser(entity)
        }
    }

    override suspend fun find(id: UInt): User {
        return suspendTransaction(database) {
            val entity = UserEntity.findById(id)
                ?: throw ResourceMissingException(User::class.java, id)
            toUser(entity)
        }
    }

    override suspend fun update(user: User) {
        suspendTransaction(database) {
            val entity = UserEntity.findById(user.id)
                ?: throw ResourceMissingException(User::class.java, user.id)
            entity.name = user.name
            entity.age = user.age
        }
    }

    override suspend fun delete(id: UInt) {
        suspendTransaction(database) {
            UserEntity.findById(id)?.delete()
        }
    }

    private fun toUser(entity: UserEntity): User {
        return User(
            id = entity.id.value,
            name = entity.name,
            age = entity.age
        )
    }
}
