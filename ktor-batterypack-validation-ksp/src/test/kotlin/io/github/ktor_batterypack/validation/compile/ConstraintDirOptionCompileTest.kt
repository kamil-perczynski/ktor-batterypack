package io.github.ktor_batterypack.validation.compile

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ConstraintDirOptionCompileTest {

    private val compiler = ValidationProcessorCompiler()

    @Test
    fun `custom constraints are loaded from yaml files and other files in the directory are ignored`(@TempDir constraintDir: Path) {
        Files.writeString(
            constraintDir.resolve("even-constraints.yaml"),
            """
            imports:
              - fixture.custom.EvenConstraints
            jsonImports:
              - fixture.custom.EvenConstraints
            defaults:
              simpleClassName: EvenConstraints
            constraints:
              - annotation: fixture.custom.Even
            """.trimIndent()
        )
        Files.write(constraintDir.resolve(".DS_Store"), byteArrayOf(0, 0, 0, 1, 66, 117, 100, 49))
        Files.writeString(constraintDir.resolve("notes.txt"), "not: [valid yaml")
        Files.createDirectory(constraintDir.resolve("nested.yaml"))

        val result = compiler.compile(
            listOf(
                SourceFile.kotlin(
                    "Counter.kt",
                    """
                    package fixture.custom

                    import io.github.ktor_batterypack.annotation.Validator
                    import io.github.ktor_batterypack.validation.SingleConstraintError
                    import io.github.ktor_batterypack.validation.ValidationCall
                    import io.github.ktor_batterypack.validation.ValidationResult

                    annotation class Even

                    object EvenConstraints {
                        @JvmStatic
                        fun checkEven(prop: String, value: Int, call: ValidationCall) {
                            if (value % 2 != 0) {
                                call.propertyError(prop, SingleConstraintError("Even"))
                            }
                        }
                    }

                    data class Counter(
                        @field:Even
                        val value: Int,
                    )

                    @Validator
                    interface CounterValidator {
                        fun validateCounter(counter: Counter): ValidationResult<Counter>
                    }

                    object Scenarios {
                        @JvmStatic
                        fun odd(): Any {
                            return CounterValidatorImpl().validateCounter(Counter(3))
                        }
                    }
                    """
                )
            ),
            processorOptions = mapOf("ktor.validation.constraint.dir" to constraintDir.toString())
        )

        assertThat(result.exitCode).withFailMessage(result.messages).isEqualTo(ExitCode.OK)
        assertThat(compiler.generatedSource(result, "CounterValidatorImpl.kt")).contains("EvenConstraints.checkEven(")
        assertThat(compiler.errorsJson(compiler.invokeStatic(result, "fixture.custom.Scenarios", "odd")))
            .isEqualTo("""{"value":[{"constraint":"Even"}]}""")
    }

}
