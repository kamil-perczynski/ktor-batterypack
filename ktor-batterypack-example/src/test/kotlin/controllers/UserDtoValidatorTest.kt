package controllers

import io.github.kperczynski.controllers.UserDtoValidator.Companion.userDtoValidator
import io.github.kperczynski.domain.user.UserCreate
import io.github.kperczynski.domain.user.UserUpdate
import io.github.ktor_batterypack.validation.ObjectConstraintError
import io.github.ktor_batterypack.validation.ValidationException
import io.github.ktor_batterypack.validation.check
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class UserDtoValidatorTest {

    @Test
    fun `should pass for valid create request`() {
        val request = UserCreate(name = "John Doe", age = 25)
        userDtoValidator.validate(request).check()
    }

    @Test
    fun `should fail for empty name on create`() {
        val request = UserCreate(name = "", age = 25)
        assertThatThrownBy { userDtoValidator.validate(request).check() }
            .isInstanceOf(ValidationException::class.java)
            .satisfies({ ex ->
                val validationEx = ex as ValidationException
                val error = validationEx.errors as ObjectConstraintError
                assertThat(error.properties).containsKey("name")
            })
    }

    @Test
    fun `should fail for short name on create`() {
        val request = UserCreate(name = "A", age = 25)
        assertThatThrownBy { userDtoValidator.validate(request).check() }
            .isInstanceOf(ValidationException::class.java)
            .satisfies({ ex ->
                val validationEx = ex as ValidationException
                val error = validationEx.errors as ObjectConstraintError
                assertThat(error.properties).containsKey("name")
            })
    }

    @Test
    fun `should pass for valid update request`() {
        val request = UserUpdate(name = "John Doe", age = 30)
        userDtoValidator.validate(request)
    }

    @Test
    fun `should fail for empty name on update`() {
        val request = UserUpdate(name = "", age = 30)
        assertThatThrownBy { userDtoValidator.validate(request).check() }
            .isInstanceOf(ValidationException::class.java)
            .satisfies({ ex ->
                val validationEx = ex as ValidationException
                val error = validationEx.errors as ObjectConstraintError
                assertThat(error.properties).containsKey("name")
            })
    }
}
