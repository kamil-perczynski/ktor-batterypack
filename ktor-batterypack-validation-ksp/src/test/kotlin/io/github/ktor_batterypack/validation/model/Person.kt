package io.github.ktor_batterypack.validation.model

import jakarta.validation.constraints.*
import java.time.LocalDate

data class Person(
    @field:NotNull
    @field:NotBlank
    val firstName: String,
    @field:NotNull
    @field:NotBlank
    val lastName: String,

    val address: Address,

    @field:Past
    val birthDate: LocalDate,

    @field:NotEmpty
    val identifications: List<PersonIdentification>,
)

data class Address(
    val addressLine1: String,
    val addressLine2: String,
    @field:NotBlank
    @field:Pattern(regexp = "\\d{2}-\\d{3}")
    @field:Size(min = 1, max = 100)
    val zipCode: String,
)

data class LegacyIdentification(override val type: PersonIdentificationType): PersonIdentification

interface PersonIdentification {
    val type: PersonIdentificationType
}

data class IdDocIdentification(
    override val type: PersonIdentificationType,
    @get:NotBlank val idNumber: String,
    @get:NotBlank val issueDate: String
) : PersonIdentification

data class LivenessIdentification(
    override val type: PersonIdentificationType,
    @get:Min(0)
    @get:Max(100)
    val confidence: Double
) : PersonIdentification

data class SignatureSpecimen1(
    override val type: PersonIdentificationType, @get:NotBlank val filePath: String, @get:Min(0)
    @get:Max(100)
    val confidence: Double
) : PersonIdentification

enum class PersonIdentificationType {
    ID_DOCUMENT_CHECK,
    LIVENESS_CHECK,
    IN_PERSON_IDENTIFICATION,
    SIGNATURE_SPECIMEN_1,
    SIGNATURE_SPECIMEN_2,
}
