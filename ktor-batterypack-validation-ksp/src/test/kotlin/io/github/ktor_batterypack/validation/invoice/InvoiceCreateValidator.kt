package io.github.ktor_batterypack.validation.invoice

import io.github.ktor_batterypack.annotation.Validator
import io.github.ktor_batterypack.validation.ValidationCall
import io.github.ktor_batterypack.validation.ValidationResult

@Validator
@Suppress("unused")
interface InvoiceCreateValidator {

    fun validateInvoiceCreate(invoiceCreate: InvoiceCreate): ValidationResult<InvoiceCreate>

    fun validateInvoicePosition(invoicePosition: InvoicePosition?, call: ValidationCall)

}