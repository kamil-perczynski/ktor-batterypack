package io.github.kperczynski.domain.user

import io.github.kperczynski.libs.exception.ErrorCodeException
import io.github.kperczynski.domain.user.UserErrorCode.USER_AGE_ILLEGAL
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(UserService::class.java)

@Singleton
class UserService(private val userRepo: UserRepo) {

    fun create(userCreate: UserCreate): User {
        checkAge(userCreate.age)

        log.info("Creating user: {}", userCreate)

        val user = User(
            id = 0u,
            name = userCreate.name,
            age = userCreate.age
        )

        return userRepo.create(user)
    }

    fun read(id: UInt): User {
        log.info("Reading user with id: {}", id)
        return userRepo.find(id)
    }

    fun update(id: UInt, userUpdate: UserUpdate) {
        log.info("Updating user with id: {} to new values: {}", id, userUpdate)
        checkAge(userUpdate.age)

        val existingUser = userRepo.find(id)

        userRepo.update(
            existingUser.copy(
                name = userUpdate.name,
                age = userUpdate.age
            )
        )
    }

    fun delete(id: UInt) {
        log.info("Deleting user with id: {}", id)
        userRepo.delete(id)
    }

}

private fun checkAge(age: Int) {
    if (age !in 1..<100) {
        throw ErrorCodeException(USER_AGE_ILLEGAL, age)
    }
}
