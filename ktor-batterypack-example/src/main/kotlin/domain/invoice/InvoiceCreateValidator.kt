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

    fun validateContractParty(contractParty: ContractParty?, call: ValidationCall) {
        if (contractParty == null) return

        when (contractParty) {
            is SellerParty -> validateSellerParty(contractParty, call)
            is BuyerParty -> validateBuyerParty(contractParty, call)
            is ReceiverParty -> validateReceiverParty(contractParty, call)
            is ProducerParty -> validateProducerParty(contractParty, call)
            is PayerParty -> validatePayerParty(contractParty, call)
        }
    }

    fun validateSellerParty(sellerParty: SellerParty?, call: ValidationCall)

    fun validatePayerParty(payerParty: PayerParty?, call: ValidationCall)

    fun validateReceiverParty(receiverParty: ReceiverParty?, call: ValidationCall)

    fun validateBuyerParty(buyerParty: BuyerParty?, call: ValidationCall)

    fun validateProducerParty(producerParty: ProducerParty?, call: ValidationCall)

}