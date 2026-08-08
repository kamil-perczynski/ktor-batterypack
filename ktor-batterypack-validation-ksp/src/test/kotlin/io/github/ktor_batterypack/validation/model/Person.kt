package io.github.ktor_batterypack.validation.model

import jakarta.validation.constraints.*
import java.time.LocalDate

data class Person(
    @field:NotNull
    @field:NotBlank
    val firstName: String?,
    @field:NotNull
    @field:NotBlank
    val lastName: String?,

    val address: Address,

    @field:Past
    val birthDate: LocalDate,

    @field:NotEmpty
    val identifications: List<PersonIdentification>
)

data class Address(
    val addressLine1: String,
    val addressLine2: String,
    @field:NotBlank
    @field:Pattern(regexp = "\\d{2}-\\d{3}")
    val zipCode: String,
)

data class PersonIdentification(
    val type: PersonIdentificationType,
    @field:Min(0)
    @field:Max(100)
    val confidence: Double
)

enum class PersonIdentificationType {
    ID_DOCUMENT_CHECK,
    LIVENESS_CHECK,
    IN_PERSON_IDENTIFICATION,
    SIGNATURE_SPECIMEN_1,
    SIGNATURE_SPECIMEN_2,
}
