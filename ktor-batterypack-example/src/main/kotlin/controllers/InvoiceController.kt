package io.github.kperczynski.controllers

import io.github.kperczynski.domain.invoice.InvoiceCreate
import io.github.kperczynski.domain.invoice.JsonInvoiceCreateValidator.Companion.jsonInvoiceCreateValidator
import io.github.ktor_batterypack.core.ktor.KtorController
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(InvoiceController::class.java)

@Singleton
class InvoiceController(private val binder: JsonBinder) : KtorController {

    override fun register(routing: Routing) {
        routing.post("/invoices") {
            val invoiceCreate =
                binder.bind<InvoiceCreate>(call, jsonInvoiceCreateValidator::validateInvoiceCreate)

            log.info("Received valid invoiceCreate=={}", invoiceCreate)

            call.respond(HttpStatusCode.OK, invoiceCreate)
        }
    }

}