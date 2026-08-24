package io.github.ktor_batterypack.validation.invoice

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.math.BigDecimal
import java.time.LocalDate

data class InvoiceCreate(
    @field:NotBlank
    val settlementNumber: String,
    val issueDate: LocalDate,
    val totalNet: BigDecimal,
    val totalVat: BigDecimal,
    val totalGross: BigDecimal,
    val totalExciseTax: BigDecimal,
    val totalEnergy: BigDecimal,
    val totalExciseEnergy: BigDecimal,
    val buyer : ContractParty,
    val payer : ContractParty,
    val receiver : ContractParty,
    @field:NotEmpty
    val positions: List<InvoicePosition>,
    @field:NotEmpty
    val meteringPointPositions: List<MeteringPointPosition>
)

data class InvoicePosition(
    @field:NotBlank
    val billingComponentCode: String,
    val billingComponentName: String?,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val unit: BillingComponentUnit,
    val net: BigDecimal,
    val vat: BigDecimal,
    val gross: BigDecimal,
    val exciseTax: BigDecimal,
)

data class MeteringPointPosition(
    @field:NotBlank
    val meteringPointCode: String,
    val address: MeteringPointAddress?,
    val billingPeriodFrom: LocalDate,
    val billingPeriodTo: LocalDate,
    @field:NotEmpty
    val positions: List<InvoicePosition>
)

data class ContractParty(val id: String)

enum class BillingComponentUnit {
    KWH, PCS
}

data class MeteringPointAddress (
    @field:NotBlank
    val addressLine1: String,
    @field:NotBlank
    val addressLine2: String,
    @field:NotBlank
    val city: String,
    @field:NotBlank
    val zipCode: String,
    @field:NotBlank
    val terytCode: String,
)