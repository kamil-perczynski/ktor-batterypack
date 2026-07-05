package io.github.kperczynski.controllers

import io.github.kperczynski.domain.user.User
import io.github.kperczynski.domain.user.UserEvent
import io.github.kperczynski.domain.user.UserEventType
import io.github.kperczynski.domain.user.UserRepo
import io.github.kperczynski.infra.CapturedMsg
import io.github.kperczynski.infra.KtorBatteriesIT
import io.github.kperczynski.infra.UserEventsMessageCollector
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.koin.ktor.plugin.koin
import tools.jackson.databind.json.JsonMapper

class UserControllerIT : KtorBatteriesIT() {

    private val userRepo: UserRepo = application.koin().get<UserRepo>()
    private val messageCollector: UserEventsMessageCollector = application.koin().get()
    private val jsonMapper: JsonMapper = application.koin().get()

    @Test
    fun `should create a user`() = runTest {
        messageCollector.expectResult()

        val response = httpClient.post("/users") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Alice","age":30}""")
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.Created)

        val captured = messageCollector.lastMessage()
        assertThat(captured).isNotNull

        val event = parseUserEvent(captured!!)
        assertThat(event.type).isEqualTo(UserEventType.USER_CREATED)
        assertThat(event.meta).containsEntry("age", "30")
    }

    @Test
    fun `should get a user`() = runBlocking {
        val createdUser = userRepo.create(User(id = 0u, name = "Bob", age = 25))
        messageCollector.expectResult()

        val response = httpClient.get("/users/${createdUser.id}")

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)

        val captured = messageCollector.lastMessage()
        assertThat(captured).isNotNull

        val event = parseUserEvent(captured!!)
        assertThat(event.type).isEqualTo(UserEventType.USER_READ)
        assertThat(event.userId).isEqualTo(createdUser.id.toString())
        assertThat(event.meta).containsEntry("age", "25")
        Unit
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

    @Test
    fun `should return 400 for invalid create request`() = runTest {
        val response = httpClient.post("/users") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"","age":25}""")
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.BadRequest)
    }

    @Test
    fun `should return 400 for invalid update request`() = runTest {
        val createdUser = userRepo.create(User(id = 0u, name = "Frank", age = 35))

        val response = httpClient.put("/users/${createdUser.id}") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"A","age":35}""")
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.BadRequest)
    }

    private fun parseUserEvent(captured: CapturedMsg): UserEvent {
        return jsonMapper.readValue(captured.payload, UserEvent::class.java)
    }

}
