package io.github.ktor_batterypack.core.ktor

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.jackson3.jackson
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.testing.testApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class DefaultKtorExceptionHandlerTest {

    private val mapper = JsonMapper()

    @Test
    fun `should respond with problem detail and keep challenge header on unauthorized`() {
        testApplication {
            install(ContentNegotiation) { jackson() }
            install(StatusPages) { DefaultKtorExceptionHandler().register(this) }
            routing {
                get("/protected") {
                    call.respond(UnauthorizedContent())
                }
            }

            val response = client.get("/protected")

            assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
            assertThat(response.headers[HttpHeaders.WWWAuthenticate]).isEqualTo("Bearer")
            assertThat(mapper.readTree(response.bodyAsText())).isEqualTo(
                mapper.readTree(
                    """
                    {
                      "type": "UNAUTHORIZED",
                      "title": "Unauthorized",
                      "status": 401,
                      "detail": "Authentication is required to access this resource",
                      "instance": "/protected"
                    }
                    """
                )
            )
        }
    }

    private class UnauthorizedContent : OutgoingContent.NoContent() {
        override val status: HttpStatusCode = HttpStatusCode.Unauthorized
        override val headers = headersOf(HttpHeaders.WWWAuthenticate, "Bearer")
    }
}
