package io.github.kperczynski.controllers

import io.github.kperczynski.domain.user.User
import io.github.kperczynski.domain.user.UserRepo
import io.github.kperczynski.infra.KtorBatteriesIT
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.koin.ktor.plugin.koin

class UserControllerIT : KtorBatteriesIT() {

    private var userRepo: UserRepo = application.koin().get<UserRepo>()

    @Test
    fun `should create a user`() = runTest {
        val response = httpClient.post("/users") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Alice","age":30}""")
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.Created)
    }

    @Test
    fun `should get a user`() = runTest {
        val createdUser = userRepo.create(User(id = 0u, name = "Bob", age = 25))

        val response = httpClient.get("/users/${createdUser.id}")

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    }

    @Test
    fun `should update a user`() = runTest {
        val createdUser = userRepo.create(User(id = 0u, name = "Charlie", age = 40))

        val response = httpClient.put("/users/${createdUser.id}") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Charles","age":41}""")
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.NoContent)
    }

    @Test
    fun `should delete a user`() = runTest {
        val createdUser = userRepo.create(User(id = 0u, name = "Dave", age = 50))

        val response = httpClient.delete("/users/${createdUser.id}")

        assertThat(response.status).isEqualTo(HttpStatusCode.NoContent)
    }

    @Test
    fun `should return 404 for missing user`() = runTest {
        val response = httpClient.get("/users/99999")


        assertThat(response.status).isEqualTo(HttpStatusCode.NotFound)
    }

    @Test
    fun `should return 422 for illegal age`() = runTest {
        val response = httpClient.post("/users") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Eve","age":150}""")
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.UnprocessableEntity)
    }

    @Test
    fun `should return 400 for invalid user id`() = runTest {
        val response = httpClient.get("/users/abc")

        assertThat(response.status).isEqualTo(HttpStatusCode.BadRequest)
    }

}
