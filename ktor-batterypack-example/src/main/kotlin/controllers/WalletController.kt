package io.github.kperczynski.controllers

import io.github.kperczynski.domain.wallet.WalletService
import io.github.kperczynski.infra.oidc.OidcAuthScheme
import io.github.ktor_batterypack.authz.PermissionChecker
import io.github.ktor_batterypack.core.ktor.KtorController
import io.ktor.http.*
import io.ktor.server.auth.authenticateWith
import io.ktor.server.auth.principal
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.ExperimentalKtorApi
import org.koin.core.annotation.Singleton

@Singleton
class WalletController(
    private val walletService: WalletService,
    private val authScheme: OidcAuthScheme,
    private val permissionChecker: PermissionChecker,
) : KtorController {

    @OptIn(ExperimentalKtorApi::class)
    override fun register(routing: Routing) {
        routing.authenticateWith(authScheme.jwt) {
            get("/api/wallets/{id}") {
                permissionChecker.checkPermission(call.principal, WALLET_READ)
                val id = call.parameters["id"]?.toUIntOrNull()
                    ?: throw IllegalArgumentException("Invalid wallet ID")

                val wallet = walletService.find(id)
                call.respond(HttpStatusCode.OK, wallet)
            }

            get("/api/wallets/topup") {
                permissionChecker.checkPermission(call.principal, WALLET_TOPUP)
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

}
