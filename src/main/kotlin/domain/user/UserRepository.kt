package io.github.kperczynski.domain.user

interface UserRepository {

    fun create(user: User): User

    fun find(id: UInt): User

    fun update(user: User)

    fun delete(id: UInt)

}
