package io.github.kperczynski.domain.user

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UserCreate(
    @field:NotBlank @field:Size(min = 2, max = 64) val name: String,
    @field:Min(0) @field:Max(150) val age: Int
)