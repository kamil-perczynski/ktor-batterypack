package io.github.kperczynski.domain.invoice

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate

data class InvoiceCreate(
    @field:NotBlank
    val settlementNumber: String,
    val issueDate: LocalDate,
    @field:Positive
    val totalNet: BigDecimal,
    @field:Positive
    val totalVat: BigDecimal,
    @field:Positive
    val totalGross: BigDecimal,
    @field:Positive
    val totalExciseTax: BigDecimal,
    @field:Positive
    val totalEnergy: BigDecimal,
    val totalExciseEnergy: BigDecimal,
    val party1: ContractParty?,
    val party2: ContractParty?,
    val party3: ContractParty?,
    val party4: ContractParty?,
    val party5: ContractParty?,
    @field:NotEmpty
    val positions: List<InvoicePosition>,
    @field:NotEmpty
    val meteringPointPositions: List<MeteringPointPosition>
)

data class InvoicePosition(
    @field:NotBlank
    val billingComponentCode: String,
    val billingComponentName: String?,
    @field:Positive
    val quantity: BigDecimal,
    @field:Positive
    val unitPrice: BigDecimal,
    val unit: BillingComponentUnit,
    @field:Positive
    val net: BigDecimal,
    @field:Positive
    val vat: BigDecimal,
    @field:Positive
    val gross: BigDecimal,
    @field:Positive
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


enum class BillingComponentUnit {
    KWH, PCS
}

data class MeteringPointAddress(
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

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(value = SellerParty::class, name = "SELLER"),
    JsonSubTypes.Type(value = BuyerParty::class, name = "BUYER"),
    JsonSubTypes.Type(value = PayerParty::class, name = "PAYER"),
    JsonSubTypes.Type(value = ReceiverParty::class, name = "RECEIVER"),
    JsonSubTypes.Type(value = ProducerParty::class, name = "PRODUCER")
)
interface ContractParty {
    @get:NotBlank
    val type: ContractPartyType

    @get:NotBlank
    val entityId: String
}
data class SellerParty(
    @field:NotBlank
    override val entityId: String,
    @field:NotBlank
    val tin: String,
    @field:NotBlank
    val name: String,
) : ContractParty {
    override val type = ContractPartyType.SELLER
}

data class BuyerParty(
    override val entityId: String,
    @field:NotBlank
    val tin: String,
    @field:NotBlank
    val name: String,
    val isPublicSector: Boolean,
) : ContractParty {
    override val type = ContractPartyType.BUYER
}

data class PayerParty(
    override val entityId: String,
    val tin: String?,
    @field:NotBlank
    val name: String,
    @field:NotBlank
    val bankAccountNumber: String,
) : ContractParty {
    override val type = ContractPartyType.PAYER
}

data class ReceiverParty(
    override val entityId: String,
    @field:NotBlank
    val name: String,
    @field:NotBlank
    val addressLine1: String,
    @field:NotBlank
    val addressLine2: String?,
    @field:NotBlank
    val postalCode: String,
    @field:NotBlank
    val city: String,
) : ContractParty {
    override val type = ContractPartyType.RECEIVER
}

data class ProducerParty(
    override val entityId: String,
    @field:NotBlank
    val name: String,
    @field:NotBlank
    val addressLine1: String,
    @field:NotBlank
    val addressLine2: String?,
    @field:NotBlank
    val postalCode: String,
    @field:NotBlank
    val city: String,
    @field:NotBlank
    val concessionNumber: String,
    val permitValidity: LocalDate,
) : ContractParty {
    override val type = ContractPartyType.PRODUCER
}

enum class ContractPartyType {
    SELLER, BUYER, PAYER, RECEIVER, PRODUCER
}