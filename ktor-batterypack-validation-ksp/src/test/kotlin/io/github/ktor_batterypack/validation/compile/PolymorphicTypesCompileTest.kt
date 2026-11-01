package io.github.ktor_batterypack.validation.compile

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class PolymorphicTypesCompileTest {

    companion object {
        private val compiler = ValidationProcessorCompiler()
        private val jsonMapper = JsonMapper.builder().build()

        private val result: JvmCompilationResult by lazy {
            compiler.compile(listOf(MODEL, VALIDATORS, SCENARIOS))
        }

        private val MODEL = SourceFile.kotlin(
            "Zoo.kt",
            """
            package fixture.poly

            import com.fasterxml.jackson.annotation.JsonSubTypes
            import com.fasterxml.jackson.annotation.JsonTypeInfo
            import com.fasterxml.jackson.annotation.JsonTypeName
            import jakarta.validation.constraints.Min
            import jakarta.validation.constraints.NotBlank
            import jakarta.validation.constraints.Size

            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
            sealed interface Animal {
                @get:NotBlank
                val name: String
            }

            @JsonTypeName("CAT")
            data class Cat(override val name: String, @field:NotBlank val favoritePrey: String) : Animal

            @JsonTypeName("DOG")
            data class Dog(override val name: String, @field:Min(0) val tricks: Int) : Animal

            sealed interface Bird : Animal

            @JsonTypeName("PARROT")
            data class Parrot(override val name: String, @field:Size(max = 3) val words: List<String>) : Bird

            data class Owl(override val name: String, @field:Min(1) val wingspan: Int) : Bird

            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
            @JsonSubTypes(
                JsonSubTypes.Type(value = Kennel::class, name = "kennel"),
                JsonSubTypes.Type(value = Stable::class, names = ["stable", "barn"])
            )
            abstract class Shelter {
                @get:Min(1)
                abstract val capacity: Int
            }

            data class Kennel(override val capacity: Int, @field:NotBlank val breed: String) : Shelter()

            data class Stable(override val capacity: Int, @field:Min(1) val horses: Int) : Shelter()

            data class Tent(override val capacity: Int) : Shelter()

            data class Zoo(
                val star: Animal,
                val animals: List<Animal>,
                val shelter: Shelter?,
            )
            """
        )

        private val VALIDATORS = SourceFile.kotlin(
            "ZooValidators.kt",
            """
            package fixture.poly

            import io.github.ktor_batterypack.annotation.JsonValidator
            import io.github.ktor_batterypack.annotation.Validator
            import io.github.ktor_batterypack.validation.ValidationParamType
            import io.github.ktor_batterypack.validation.ValidationResult
            import tools.jackson.databind.JsonNode

            @Validator
            interface ZooValidator {
                fun validateZoo(zoo: Zoo): ValidationResult<Zoo>
            }

            @JsonValidator
            interface JsonZooValidator {
                @ValidationParamType(Zoo::class)
                fun validateZoo(zoo: JsonNode): ValidationResult<JsonNode>
            }
            """
        )

        private val SCENARIOS = SourceFile.kotlin(
            "Scenarios.kt",
            """
            package fixture.poly

            import tools.jackson.databind.json.JsonMapper

            object Scenarios {

                @JvmStatic
                fun typed(zoo: Zoo): Any {
                    return ZooValidatorImpl().validateZoo(zoo)
                }

                @JvmStatic
                fun valid(): Any {
                    return typed(
                        Zoo(
                            star = Cat("Tom", "mouse"),
                            animals = listOf(Dog("Rex", 3), Parrot("Polly", listOf("hi")), Owl("Hedwig", 2)),
                            shelter = Kennel(2, "husky"),
                        )
                    )
                }

                @JvmStatic
                fun invalid(): Any {
                    return typed(
                        Zoo(
                            star = Cat(" ", ""),
                            animals = listOf(Dog("Rex", -1), Parrot("Polly", listOf("a", "b", "c", "d")), Owl("", 0)),
                            shelter = Stable(0, 0),
                        )
                    )
                }

                @JvmStatic
                fun unlistedSubtype(): Any {
                    return typed(Zoo(star = Dog("Rex", 1), animals = emptyList(), shelter = Tent(0)))
                }

                @JvmStatic
                fun json(json: String): Any {
                    return JsonZooValidatorImpl().validateZoo(JsonMapper.builder().build().readTree(json))
                }
            }
            """
        )
    }

    @Test
    fun `validators for polymorphic types compile`() {
        assertThat(result.exitCode).withFailMessage(result.messages).isEqualTo(ExitCode.OK)
    }

    @Test
    fun `typed validator dispatches sealed types with an exhaustive when`() {
        assertThat(compiler.generatedSource(result, "ZooValidatorImpl.kt")).contains(
            """
                |  private fun validateAnimal(animal: Animal?, call: ValidationCall) {
                |    if (animal == null) return
                |    when (animal) {
                |      is Bird -> validateBird(animal, call)
                |      is Cat -> validateCat(animal, call)
                |      is Dog -> validateDog(animal, call)
                |    }
                |  }
                |
                |  private fun validateBird(bird: Bird?, call: ValidationCall) {
                |    if (bird == null) return
                |    when (bird) {
                |      is Owl -> validateOwl(bird, call)
                |      is Parrot -> validateParrot(bird, call)
                |    }
                |  }
            """.trimMargin().replace("  ", "    ")
        )
    }

    @Test
    fun `typed validator accepts valid subtypes`() {
        val validationResult = compiler.invokeStatic(result, "fixture.poly.Scenarios", "valid")

        assertThat(compiler.errorsJson(validationResult)).isNull()
    }

    @Test
    fun `typed validator checks subtype and inherited constraints`() {
        val validationResult = compiler.invokeStatic(result, "fixture.poly.Scenarios", "invalid")

        assertThat(readTree(compiler.errorsJson(validationResult))).isEqualTo(
            readTree(
                """
                {
                  "star": {
                    "name": [{"constraint": "NotBlank"}],
                    "favoritePrey": [{"constraint": "NotBlank"}]
                  },
                  "animals": {
                    "errors": [],
                    "items": [
                      {"tricks": [{"constraint": "Min", "message": "Must be at least 0"}]},
                      {"words": {"errors": [{"constraint": "Size", "message": "Must have size between [0, 3]"}], "items": [null, null, null, null]}},
                      {
                        "name": [{"constraint": "NotBlank"}],
                        "wingspan": [{"constraint": "Min", "message": "Must be at least 1"}]
                      }
                    ]
                  },
                  "shelter": {
                    "capacity": [{"constraint": "Min", "message": "Must be at least 1"}],
                    "horses": [{"constraint": "Min", "message": "Must be at least 1"}]
                  }
                }
                """
            )
        )
    }

    @Test
    fun `typed validator falls back to declared properties for unlisted subtypes`() {
        val validationResult = compiler.invokeStatic(result, "fixture.poly.Scenarios", "unlistedSubtype")

        assertThat(readTree(compiler.errorsJson(validationResult))).isEqualTo(
            readTree("""{"shelter": {"capacity": [{"constraint": "Min", "message": "Must be at least 1"}]}}""")
        )
    }

    @Test
    fun `json validator accepts valid subtypes`() {
        val validationResult = json(
            """
            {
              "star": {"kind": "CAT", "name": "Tom", "favoritePrey": "mouse"},
              "animals": [
                {"kind": "DOG", "name": "Rex", "tricks": 3},
                {"kind": "PARROT", "name": "Polly", "words": ["hi"]},
                {"kind": "Owl", "name": "Hedwig", "wingspan": 2}
              ],
              "shelter": {"@type": "barn", "capacity": 2, "horses": 1}
            }
            """
        )

        assertThat(compiler.errorsJson(validationResult)).isNull()
    }

    @Test
    fun `json validator reports unknown and missing type ids`() {
        val validationResult = json(
            """
            {
              "star": {"kind": "COW", "name": "Daisy"},
              "animals": [{"name": "Rex", "tricks": 3}],
              "shelter": {"@type": 7, "capacity": 2}
            }
            """
        )

        assertThat(readTree(compiler.errorsJson(validationResult))).isEqualTo(
            readTree(
                """
                {
                  "star": {
                    "kind": [{"constraint": "EnumValues", "message": "Allowed values are: [CAT, DOG, Owl, PARROT]"}]
                  },
                  "animals": {
                    "errors": [],
                    "items": [{"kind": [{"constraint": "NotNull", "message": "Must not be null"}]}]
                  },
                  "shelter": {
                    "@type": [{"constraint": "Type", "message": "Expected string but was number"}]
                  }
                }
                """
            )
        )
    }

    @Test
    fun `json validator checks the selected subtype`() {
        val validationResult = json(
            """
            {
              "star": {"kind": "CAT", "name": " ", "favoritePrey": ""},
              "animals": [{"kind": "Owl", "name": "Hedwig", "wingspan": 0}],
              "shelter": {"@type": "kennel", "capacity": 0, "breed": ""}
            }
            """
        )

        assertThat(readTree(compiler.errorsJson(validationResult))).isEqualTo(
            readTree(
                """
                {
                  "star": {
                    "name": [{"constraint": "NotBlank"}],
                    "favoritePrey": [{"constraint": "NotBlank"}]
                  },
                  "animals": {
                    "errors": [],
                    "items": [{"wingspan": [{"constraint": "Min", "message": "Must be at least 1"}]}]
                  },
                  "shelter": {
                    "capacity": [{"constraint": "Min", "message": "Must be at least 1"}],
                    "breed": [{"constraint": "NotBlank"}]
                  }
                }
                """
            )
        )
    }

    @Test
    fun `unsupported polymorphic mappings fail the build`() {
        val failed = compiler.compile(
            listOf(
                SourceFile.kotlin(
                    "Broken.kt",
                    """
                    package fixture.poly.broken

                    import com.fasterxml.jackson.annotation.JsonSubTypes
                    import com.fasterxml.jackson.annotation.JsonTypeInfo
                    import com.fasterxml.jackson.annotation.JsonTypeName
                    import io.github.ktor_batterypack.annotation.JsonValidator
                    import io.github.ktor_batterypack.annotation.Validator
                    import io.github.ktor_batterypack.validation.ValidationParamType
                    import io.github.ktor_batterypack.validation.ValidationResult
                    import tools.jackson.databind.JsonNode

                    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
                    sealed interface Shape
                    data class Circle(val radius: Int) : Shape
                    data class Square(val side: Int) : Shape

                    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
                    sealed interface Vehicle
                    @JsonTypeName("car") data class Car(val seats: Int) : Vehicle
                    @JsonTypeName("car") data class Van(val seats: Int) : Vehicle

                    @JsonSubTypes(JsonSubTypes.Type(value = Circle::class, name = "circle"))
                    interface Plant

                    sealed interface Box
                    data class Crate<T>(val content: T) : Box

                    data class Garage(val shape: Shape, val vehicle: Vehicle)

                    data class Yard(val plant: Plant, val box: Box)

                    @JsonValidator
                    interface GarageValidator {
                        @ValidationParamType(Garage::class)
                        fun validateGarage(garage: JsonNode): ValidationResult<JsonNode>
                    }

                    @Validator
                    interface YardValidator {
                        fun validateYard(yard: Yard): ValidationResult<Yard>
                    }
                    """
                )
            )
        )

        assertThat(failed.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(failed.messages)
            .contains("@JsonTypeInfo(use = CLASS, include = PROPERTY) of `Shape` is not supported by a @JsonValidator")
            .contains("Type id \"car\" of `Vehicle` maps to both `Car` and `Van`")
            .contains("`Circle` is listed in @JsonSubTypes of `Plant` but does not extend it")
            .contains("Subtype `Crate` of `Box` has unsupported type `Crate<*>`")
    }

    private fun json(json: String): Any? {
        return compiler.invokeStatic(result, "fixture.poly.Scenarios", "json", json)
    }

    private fun readTree(json: String?): Any? {
        if (json == null) {
            return null
        }
        return jsonMapper.readTree(json)
    }

}
