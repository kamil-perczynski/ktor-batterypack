package io.github.kperczynski

import io.github.kperczynski.repository.User
import io.github.kperczynski.repository.UsersRepository
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(UserService::class.java)

@Singleton
class UserService(private val usersRepository: UsersRepository) {

    fun create(user: User): UInt {
        log.info("Creating user: {}", user)
        return usersRepository.create(user)
    }

    fun read(id: UInt): User {
        log.info("Reading user with id: {}", id)
        return usersRepository.read(id)
    }

    fun update(id: UInt, user: User) {
        log.info("Updating user with id: {} to new values: {}", id, user)
        usersRepository.update(id, user)
    }

    fun delete(id: UInt) {
        log.info("Deleting user with id: {}", id)
        usersRepository.delete(id)
    }

}
