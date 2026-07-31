package io.github.ktor_batterypack.validation.ksp

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ValidationGeneratorTest {

    @Test
    fun `should render validator implementation from model`() {
        val model = ValidationModel(
            packageName = "io.github.kperczynski.controllers",
            interfaceSimpleName = "KspUserDtoValidator",
            implSimpleName = "KspUserDtoValidatorImpl",
            imports = listOf(
                "io.github.kperczynski.domain.user.UserCreate",
                "io.github.kperczynski.domain.user.UserUpdate"
            ),
            methods = listOf(
                MethodModel(
                    name = "validateCreate",
                    paramName = "value",
                    dtoSimpleName = "UserCreate",
                    properties = listOf(
                        PropertyModel(
                            name = "name",
                            typeName = "kotlin.String",
                            isString = true,
                            isNumeric = false,
                            isBoolean = false,
                            isNullable = false,
                            accessor = "name",
                            nested = emptyList()
                        ),
                        PropertyModel(
                            name = "age",
                            typeName = "kotlin.Int",
                            isString = false,
                            isNumeric = true,
                            isBoolean = false,
                            isNullable = false,
                            accessor = "age",
                            nested = emptyList()
                        )
                    )
                ),
                MethodModel(
                    name = "validateUpdate",
                    paramName = "value",
                    dtoSimpleName = "UserUpdate",
                    properties = listOf(
                        PropertyModel(
                            name = "name",
                            typeName = "kotlin.String",
                            isString = true,
                            isNumeric = false,
                            isBoolean = false,
                            isNullable = false,
                            accessor = "name",
                            nested = emptyList()
                        ),
                        PropertyModel(
                            name = "age",
                            typeName = "kotlin.Int",
                            isString = false,
                            isNumeric = true,
                            isBoolean = false,
                            isNullable = false,
                            accessor = "age",
                            nested = emptyList()
                        )
                    )
                )
            )
        )

        val loader = ClassPathTemplateLoader("/")
        val handlebars = Handlebars(loader)
        val template = handlebars.compile("validation-impl")
        val rendered = template.apply(model)

        val expected = """
            package io.github.kperczynski.controllers

            import io.github.kperczynski.domain.user.UserCreate
            import io.github.kperczynski.domain.user.UserUpdate
            
            class KspUserDtoValidatorImpl : KspUserDtoValidator {
            
                override fun validateCreate(value: UserCreate) {
                    require(value.name.isNotBlank()) { "name must not be blank" }
                    require(value.age > 0) { "age must be positive" }
                }
            
                override fun validateUpdate(value: UserUpdate) {
                    require(value.name.isNotBlank()) { "name must not be blank" }
                    require(value.age > 0) { "age must be positive" }
                }
            
            }
            
        """.trimIndent()

        assertThat(rendered).isEqualTo(expected)
    }

    @Test
    fun `should render nullable property validators`() {
        val model = ValidationModel(
            packageName = "io.github.kperczynski.controllers",
            interfaceSimpleName = "KspUserDtoValidator",
            implSimpleName = "KspUserDtoValidatorImpl",
            imports = listOf(
                "io.github.kperczynski.domain.user.UserCreate"
            ),
            methods = listOf(
                MethodModel(
                    name = "validateCreate",
                    paramName = "value",
                    dtoSimpleName = "UserCreate",
                    properties = listOf(
                        PropertyModel(
                            name = "name",
                            typeName = "kotlin.String",
                            isString = true,
                            isNumeric = false,
                            isBoolean = false,
                            isNullable = true,
                            accessor = "name",
                            nested = emptyList()
                        ),
                        PropertyModel(
                            name = "age",
                            typeName = "kotlin.Int",
                            isString = false,
                            isNumeric = true,
                            isBoolean = false,
                            isNullable = true,
                            accessor = "age",
                            nested = emptyList()
                        )
                    )
                )
            )
        )

        val loader = ClassPathTemplateLoader("/")
        val handlebars = Handlebars(loader)
        val template = handlebars.compile("validation-impl")
        val rendered = template.apply(model)

        val expected = """
            package io.github.kperczynski.controllers

            import io.github.kperczynski.domain.user.UserCreate

            class KspUserDtoValidatorImpl : KspUserDtoValidator {

                override fun validateCreate(value: UserCreate) {
                    require(value.name == null || value.name?.isNotBlank() == true) { "name must not be blank" }
                    require(value.age == null || value.age > 0) { "age must be positive" }
                }

            }
            
        """.trimIndent()

        assertThat(rendered).isEqualTo(expected)
    }
}
