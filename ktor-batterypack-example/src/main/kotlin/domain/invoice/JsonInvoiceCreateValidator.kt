package io.github.kperczynski.domain.invoice

import io.github.ktor_batterypack.annotation.JsonValidator
import io.github.ktor_batterypack.validation.*
import io.github.ktor_batterypack.validation.JsonTypeChecks.checkLocalDate
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode

@JsonValidator
interface JsonInvoiceCreateValidator {

    companion object {
        val jsonInvoiceCreateValidator: JsonInvoiceCreateValidator =
            JsonInvoiceCreateValidatorImpl()
    }

    @ValidationParamType(InvoiceCreate::class)
    fun validateInvoiceCreate(invoiceCreate: JsonNode): ValidationResult<JsonNode>

    @ValidationParamType(MeteringPointPosition::class)
    fun checkBillingPeriod(meteringPointPosition: ObjectNode, call: ValidationCall) {
        val billingPeriodFrom = checkLocalDate("billingPeriodFrom", meteringPointPosition)
        val billingPeriodTo = checkLocalDate("billingPeriodTo", meteringPointPosition)

        if (billingPeriodFrom == null || billingPeriodTo == null) {
            return
        }

        if (billingPeriodFrom.isAfter(billingPeriodTo)) {
            call.propertyError(
                "billingPeriodFrom",
                SingleConstraintError("TimeMath", "Billing period dates are incorrect")
            )
        }
    }

    @ValidationParamType(ContractParty::class)
    fun validateContractParty(contractParty: ObjectNode?, call: ValidationCall) {
        if (contractParty == null) return

        JsonTypeChecks.checkNotNull("type", contractParty, call)
        val type = JsonTypeChecks.checkString("type", contractParty, call) ?: return

        when (type) {
            ContractPartyType.SELLER.name -> validateSellerParty(contractParty, call)
            ContractPartyType.BUYER.name -> validateBuyerParty(contractParty, call)
            ContractPartyType.RECEIVER.name -> validateReceiverParty(contractParty, call)
            ContractPartyType.RECEIVER.name -> validateProducerParty(contractParty, call)
            ContractPartyType.PRODUCER.name -> validatePayerParty(contractParty, call)
            else -> call.propertyError(
                "type",
                SingleConstraintError(
                    "EnumConstant",
                    "Unrecognized contract party type '$type'"
                )
            )
        }
    }

    @ValidationParamType(InvoiceCreate::class)
    fun checkInvoiceCreate(invoiceCreate: ObjectNode?, call: ValidationCall) {
        if (invoiceCreate == null) return

        val types = mutableSetOf<String?>()
        types.add(stringAtPointer(invoiceCreate, "/party1/type"))
        types.add(stringAtPointer(invoiceCreate, "/party2/type"))
        types.add(stringAtPointer(invoiceCreate, "/party3/type"))
        types.add(stringAtPointer(invoiceCreate, "/party4/type"))
        types.add(stringAtPointer(invoiceCreate, "/party5/type"))

        if (!types.contains(ContractPartyType.BUYER.name)) {
            call.propertyError(
                "$",
                SingleConstraintError("BuyerRequired", "Invoice must have a buyer party defined")
            )
        }
        if (!types.contains(ContractPartyType.SELLER.name)) {
            call.propertyError(
                "$",
                SingleConstraintError("SellerRequired", "Invoice must have a seller party defined")
            )
        }
    }

    @ValidationParamType(SellerParty::class)
    fun validateSellerParty(sellerParty: ObjectNode?, call: ValidationCall)

    @ValidationParamType(PayerParty::class)
    fun validatePayerParty(payerParty: ObjectNode?, call: ValidationCall)

    @ValidationParamType(ReceiverParty::class)
    fun validateReceiverParty(receiverParty: ObjectNode?, call: ValidationCall)

    @ValidationParamType(BuyerParty::class)
    fun validateBuyerParty(buyerParty: ObjectNode?, call: ValidationCall)

    @ValidationParamType(ProducerParty::class)
    fun validateProducerParty(producerParty: ObjectNode?, call: ValidationCall)

}

private fun stringAtPointer(invoiceCreate: ObjectNode?, expr: String): String? {
    val nodes = invoiceCreate?.at(expr)

    if (nodes is StringNode) {
        return nodes.asString()
    }

    return null
}