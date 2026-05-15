package io.github.kperczynski.domain.user

interface UserRepo {

    suspend fun create(user: User): User

    suspend fun find(id: UInt): User

    suspend fun update(user: User)

    suspend fun delete(id: UInt)

}
