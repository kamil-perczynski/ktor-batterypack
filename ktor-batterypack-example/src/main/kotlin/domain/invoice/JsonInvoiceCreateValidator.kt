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

}

private fun stringAtPointer(invoiceCreate: ObjectNode?, expr: String): String? {
    val nodes = invoiceCreate?.at(expr)

    if (nodes is StringNode) {
        return nodes.asString()
    }

    return null
}