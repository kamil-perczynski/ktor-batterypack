package io.github.kperczynski.controllers

import io.github.kperczynski.domain.invoice.InvoiceCreate
import io.github.kperczynski.domain.invoice.JsonInvoiceCreateValidator.Companion.jsonInvoiceCreateValidator
import io.github.kperczynski.infra.oidc.OidcAuthScheme
import io.github.ktor_batterypack.authz.PermissionChecker
import io.github.ktor_batterypack.core.ktor.JsonBinder
import io.github.ktor_batterypack.core.ktor.KtorController
import io.ktor.http.*
import io.ktor.server.auth.authenticateWith
import io.ktor.server.auth.principal
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.ExperimentalKtorApi
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(InvoiceController::class.java)

@Singleton
class InvoiceController(
    private val binder: JsonBinder,
    private val authScheme: OidcAuthScheme,
    private val permissionChecker: PermissionChecker,
) : KtorController {

    @OptIn(ExperimentalKtorApi::class)
    override fun register(routing: Routing) {
        routing.authenticateWith(authScheme.jwt) {
            post("/invoices") {
                permissionChecker.checkPermission(call.principal, INVOICE_CREATE)
                val invoiceCreate =
                    binder.bindBody<InvoiceCreate>(call, jsonInvoiceCreateValidator::validateInvoiceCreate)

                log.info("Received valid invoiceCreate=={}", invoiceCreate)

                call.respond(HttpStatusCode.OK, invoiceCreate)
            }
        }
    }

}