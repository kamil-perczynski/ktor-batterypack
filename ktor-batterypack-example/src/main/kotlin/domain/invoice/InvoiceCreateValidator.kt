package io.github.kperczynski.domain.invoice

import io.github.ktor_batterypack.annotation.Validator
import io.github.ktor_batterypack.validation.SingleConstraintError
import io.github.ktor_batterypack.validation.ValidationCall
import io.github.ktor_batterypack.validation.ValidationResult

@Validator
interface InvoiceCreateValidator {

    fun validateInvoiceCreate(invoiceCreate: InvoiceCreate): ValidationResult<InvoiceCreate>

    fun checkInvoiceCreate(invoiceCreate: InvoiceCreate?, call: ValidationCall) {
        if (invoiceCreate == null) return

        val types = mutableSetOf<ContractPartyType?>()
        types.add(invoiceCreate.party1?.type)
        types.add(invoiceCreate.party2?.type)
        types.add(invoiceCreate.party3?.type)
        types.add(invoiceCreate.party4?.type)
        types.add(invoiceCreate.party5?.type)

        if (!types.contains(ContractPartyType.BUYER)) {
            call.propertyError(
                "$",
                SingleConstraintError("BuyerRequired", "Invoice must have a buyer party defined")
            )
        }
        if (!types.contains(ContractPartyType.SELLER)) {
            call.propertyError(
                "$",
                SingleConstraintError("SellerRequired", "Invoice must have a seller party defined")
            )
        }
    }

}