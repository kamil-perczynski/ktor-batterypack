package controllers

import io.github.kperczynski.controllers.UserDtoValidator
import io.github.kperczynski.domain.user.UserCreate
import io.github.kperczynski.domain.user.UserUpdate
import io.github.kperczynski.libs.exception.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class UserDtoValidatorTest {

    private val validator = UserDtoValidator()

    @Test
    fun `should pass for valid create request`() {
        val request = UserCreate(name = "John Doe", age = 25)
        validator.validateCreate(request)
    }

    @Test
    fun `should fail for empty name on create`() {
        val request = UserCreate(name = "", age = 25)
        assertThatThrownBy { validator.validateCreate(request) }
            .isInstanceOf(ValidationException::class.java)
            .satisfies({ ex ->
                val validationEx = ex as ValidationException
                assertThat(validationEx.errors).anyMatch { it.field.contains("name") }
            })
    }

    @Test
    fun `should fail for short name on create`() {
        val request = UserCreate(name = "A", age = 25)
        assertThatThrownBy { validator.validateCreate(request) }
            .isInstanceOf(ValidationException::class.java)
            .satisfies({ ex ->
                val validationEx = ex as ValidationException
                assertThat(validationEx.errors).anyMatch { it.field.contains("name") }
            })
    }

    @Test
    fun `should pass for valid update request`() {
        val request = UserUpdate(name = "John Doe", age = 30)
        validator.validateUpdate(request)
    }

    @Test
    fun `should fail for empty name on update`() {
        val request = UserUpdate(name = "", age = 30)
        assertThatThrownBy { validator.validateUpdate(request) }
            .isInstanceOf(ValidationException::class.java)
            .satisfies({ ex ->
                val validationEx = ex as ValidationException
                assertThat(validationEx.errors).anyMatch { it.field.contains("name") }
            })
    }
}
