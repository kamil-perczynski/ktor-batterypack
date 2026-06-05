package io.github.kperczynski.domain.user

import io.github.kperczynski.domain.user.UserErrorCode.INVALID_USER_AGE
import io.github.ktor_batterypack.core.exception.ErrorCodeException
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(UserService::class.java)

@Singleton
class UserService(
    private val userRepo: UserRepo,
    private val userEventPublisher: UserEventPublisher
) {

    suspend fun create(userCreate: UserCreate): User {
        checkAge(userCreate.age)

        log.info("Creating user: {}", userCreate)

        val user = User(
            id = 0u,
            name = userCreate.name,
            age = userCreate.age
        )

        val createdUser = userRepo.create(user)
        userEventPublisher.publish(
            UserEvent(
                userId = createdUser.id.toString(),
                type = UserEventType.USER_CREATED,
                meta = mapOf("age" to createdUser.age.toString())
            )
        )
        return createdUser
    }

    suspend fun read(id: UInt): User {
        log.info("Reading user with id: {}", id)
        val foundUser = userRepo.find(id)
        userEventPublisher.publish(
            UserEvent(
                userId = foundUser.id.toString(),
                type = UserEventType.USER_READ,
                meta = mapOf("age" to foundUser.age.toString())
            )
        )
        return foundUser
    }

    suspend fun update(id: UInt, userUpdate: UserUpdate) {
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

    suspend fun delete(id: UInt) {
        log.info("Deleting user with id: {}", id)
        userRepo.delete(id)
    }

}

private fun checkAge(age: Int) {
    if (age !in 1..<100) {
        throw ErrorCodeException(INVALID_USER_AGE, age)
    }
}
