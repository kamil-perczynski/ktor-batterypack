package io.github.ktor_batterypack.validation.codegen.constraint

import com.squareup.kotlinpoet.MemberName

data class ConstraintCall(
    val member: MemberName,
    val args: Map<String, Any>,
    val rejectsNull: Boolean
)
