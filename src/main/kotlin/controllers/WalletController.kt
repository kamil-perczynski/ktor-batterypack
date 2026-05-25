package io.github.kperczynski.controllers

import io.github.kperczynski.domain.wallet.WalletService
import io.github.kperczynski.libs.ktor.KtorController
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.annotation.Singleton

@Singleton
class WalletController(private val walletService: WalletService) : KtorController {

    override fun register(routing: Routing) {
        routing.get("/api/wallets/{id}") {
            val id = call.parameters["id"]?.toUIntOrNull()
                ?: throw IllegalArgumentException("Invalid wallet ID")

            val wallet = walletService.find(id)
            call.respond(HttpStatusCode.OK, wallet)
        }

        routing.get("/api/wallets/topup") {
            val walletId = call.request.queryParameters["walletId"]
                ?.let { it.toUIntOrNull() ?: throw IllegalArgumentException("Invalid walletId") }
            val userId = call.request.queryParameters["userId"]
                ?.let { it.toUIntOrNull() ?: throw IllegalArgumentException("Invalid userId") }
            val amount = call.request.queryParameters["amount"]
                ?.toBigDecimalOrNull()
                ?: throw IllegalArgumentException("Invalid amount")

            val wallet = walletService.requestTopup(amount, walletId, userId)
            call.respond(HttpStatusCode.Accepted, wallet)
        }
    }

}
