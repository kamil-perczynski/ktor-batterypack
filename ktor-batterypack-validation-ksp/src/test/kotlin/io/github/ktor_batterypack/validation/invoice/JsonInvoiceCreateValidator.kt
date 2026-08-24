package io.github.ktor_batterypack.validation.invoice

import io.github.ktor_batterypack.annotation.JsonValidator
import io.github.ktor_batterypack.validation.JsonConstraints.checkLocalDate
import io.github.ktor_batterypack.validation.SingleConstraintError
import io.github.ktor_batterypack.validation.ValidationCall
import io.github.ktor_batterypack.validation.ValidationParamType
import io.github.ktor_batterypack.validation.ValidationResult
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode

@Suppress("unused")
@JsonValidator
interface JsonInvoiceCreateValidator {

    @ValidationParamType(InvoiceCreate::class)
    fun validateInvoiceCreate(invoiceCreate: JsonNode): ValidationResult<JsonNode>

    @ValidationParamType(InvoicePosition::class)
    fun validateInvoicePosition(invoicePosition: ObjectNode?, call: ValidationCall)

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

}