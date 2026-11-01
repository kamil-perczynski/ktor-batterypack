package io.github.ktor_batterypack.authz

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class JwtAuthFactoryTest {

    @Test
    fun `should resolve id and groups from default claims`() {
        val factory = JwtAuthFactory(JwtAuthProps())
        val claims = buildJsonObject {
            put("sub", "user-1")
            put("groups", buildJsonArray {
                add(JsonPrimitive("admins"))
                add(JsonPrimitive("users"))
            })
        }

        val auth = factory.create(claims)

        assertThat(auth).isEqualTo(JwtAuth(id = "user-1", groups = setOf("admins", "users")))
    }

    @Test
    fun `should resolve groups from configured claim`() {
        val factory = JwtAuthFactory(JwtAuthProps(groupsClaim = "cognito:groups"))
        val claims = buildJsonObject {
            put("sub", "user-1")
            put("groups", buildJsonArray { add(JsonPrimitive("ignored")) })
            put("cognito:groups", buildJsonArray { add(JsonPrimitive("admins")) })
        }

        val auth = factory.create(claims)

        assertThat(auth.groups).containsExactly("admins")
    }

    @Test
    fun `should resolve id from configured claim`() {
        val factory = JwtAuthFactory(JwtAuthProps(idClaim = "username"))
        val claims = buildJsonObject {
            put("sub", "user-1")
            put("username", "john")
        }

        val auth = factory.create(claims)

        assertThat(auth.id).isEqualTo("john")
    }

    @Test
    fun `should resolve empty groups when claim is missing`() {
        val factory = JwtAuthFactory(JwtAuthProps())
        val claims = buildJsonObject { put("sub", "user-1") }

        val auth = factory.create(claims)

        assertThat(auth.groups).isEmpty()
    }

    @Test
    fun `should resolve single group from string claim`() {
        val factory = JwtAuthFactory(JwtAuthProps())
        val claims = buildJsonObject {
            put("sub", "user-1")
            put("groups", "admins")
        }

        val auth = factory.create(claims)

        assertThat(auth.groups).containsExactly("admins")
    }

    @Test
    fun `should fail when id claim is missing`() {
        val factory = JwtAuthFactory(JwtAuthProps())
        val claims = buildJsonObject { put("groups", "admins") }

        assertThatThrownBy { factory.create(claims) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("sub")
    }
}
