package io.github.ktor_batterypack.authz

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class PermissionCheckerTest {

    private val checker = PermissionChecker(
        AuthzProps(
            groups = mapOf(
                "admins" to AuthzGroupProps(permissions = listOf("plant:list", "plant:read", "wallet:topup")),
                "users" to AuthzGroupProps(permissions = listOf("plant:list", "plant:read")),
                "auditors" to AuthzGroupProps(permissions = listOf("audit-log", "report:export")),
            )
        )
    )

    @Test
    fun `should grant permission assigned to group`() {
        val auth = JwtAuth(id = "user-1", groups = setOf("users"))

        assertThat(checker.hasPermission(auth, "plant:read")).isTrue()
        assertThat(checker.hasPermission(auth, "wallet:topup")).isFalse()
    }

    @Test
    fun `should union permissions across groups`() {
        val auth = JwtAuth(id = "user-1", groups = setOf("users", "auditors"))

        assertThat(checker.findPermissions(auth))
            .containsExactlyInAnyOrder("plant:list", "plant:read", "audit-log", "report:export")
    }

    @Test
    fun `should grant no permissions for unknown group`() {
        val auth = JwtAuth(id = "user-1", groups = setOf("unknown"))

        assertThat(checker.findPermissions(auth)).isEmpty()
        assertThat(checker.hasPermission(auth, "plant:list")).isFalse()
    }

    @Test
    fun `should support arbitrary permission strings`() {
        val auth = JwtAuth(id = "user-1", groups = setOf("auditors"))

        assertThat(checker.hasPermission(auth, "audit-log")).isTrue()
    }

    @Test
    fun `should pass check when permission is granted`() {
        val auth = JwtAuth(id = "user-1", groups = setOf("admins"))

        checker.checkPermission(auth, "wallet:topup")
    }

    @Test
    fun `should throw when permission is missing`() {
        val auth = JwtAuth(id = "user-1", groups = setOf("users"))

        assertThatThrownBy { checker.checkPermission(auth, "wallet:topup") }
            .isInstanceOf(PermissionDeniedException::class.java)
            .hasMessageContaining("wallet:topup")
    }
}
