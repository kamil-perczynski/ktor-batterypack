package io.github.kperczynski.domain.user

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class UserUpdate(
    @field:NotEmpty @field:Size(min = 2) val name: String,
    @field:Min(0) @field:Max(150) val age: Int
)