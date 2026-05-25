package io.github.kperczynski.controllers

import io.github.kperczynski.domain.user.User
import io.github.kperczynski.domain.user.UserRepo
import io.github.kperczynski.domain.wallet.Wallet
import io.github.kperczynski.domain.wallet.WalletEvent
import io.github.kperczynski.domain.wallet.WalletEventType
import io.github.kperczynski.domain.wallet.WalletRepo
import io.github.kperczynski.infra.KtorBatteriesIT
import io.github.kperczynski.infra.WalletEventsMessageCollector
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.koin.ktor.plugin.koin
import java.math.BigDecimal

class WalletControllerIT : KtorBatteriesIT() {

    private val userRepo: UserRepo = application.koin().get()
    private val walletRepo: WalletRepo = application.koin().get()
    private val messageCollector: WalletEventsMessageCollector = application.koin().get()

    @Test
    fun `should topup existing wallet when walletId provided`() = runBlocking {
        // given an existing user and wallet with balance 100.00
        val user = userRepo.create(User(id = 0u, name = "Alice", age = 30))
        val wallet = walletRepo.create(
            Wallet(id = 0u, userId = user.id, balance = BigDecimal("100.00"))
        )
        messageCollector.expectResult(count = 2)

        // when requesting topup by walletId
        val response = httpClient.get("/api/wallets/topup?amount=50.00&walletId=${wallet.id}")

        // then HTTP 202 is returned with the wallet before topup
        assertThat(response.status).isEqualTo(HttpStatusCode.Accepted)
        val responseWallet = response.body<Wallet>()
        assertThat(responseWallet).isEqualTo(wallet)

        // and both events are published
        val events = messageCollector.awaitMessages()
        assertThat(events).containsExactly(
            WalletEvent(
                walletId = wallet.id.toString(),
                amount = "50.00",
                userId = user.id.toString(),
                type = WalletEventType.WALLET_TOPUP_REQUESTED
            ),
            WalletEvent(
                walletId = wallet.id.toString(),
                amount = "150.00",
                userId = user.id.toString(),
                type = WalletEventType.WALLET_BALANCE_CHANGED
            )
        )

        // and the wallet balance in DB is updated to 150.00
        val updatedWallet = walletRepo.find(wallet.id)
        assertThat(updatedWallet).isNotNull
        assertThat(updatedWallet!!.balance).isEqualByComparingTo(BigDecimal("150.00"))
        assertThat(updatedWallet.userId).isEqualTo(user.id)
        Unit
    }

    @Test
    fun `should create new wallet when only userId provided and wallet already exists`() = runBlocking {
        // given an existing user who already has a wallet with balance 200.00
        val user = userRepo.create(User(id = 0u, name = "Bob", age = 25))
        walletRepo.create(Wallet(id = 0u, userId = user.id, balance = BigDecimal("200.00")))
        messageCollector.expectResult(count = 2)

        // when requesting topup by userId only
        val response = httpClient.get("/api/wallets/topup?amount=25.00&userId=${user.id}")

        // then HTTP 202 is returned with a newly created wallet having zero balance
        assertThat(response.status).isEqualTo(HttpStatusCode.Accepted)
        val responseWallet = response.body<Wallet>()
        assertThat(responseWallet.balance).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(responseWallet.userId).isEqualTo(user.id)

        // and both events are published with the new walletId
        val events = messageCollector.awaitMessages()
        val walletId = responseWallet.id.toString()
        assertThat(events).containsExactly(
            WalletEvent(
                walletId = walletId,
                amount = "25.00",
                userId = user.id.toString(),
                type = WalletEventType.WALLET_TOPUP_REQUESTED
            ),
            WalletEvent(
                walletId = walletId,
                amount = "25.00",
                userId = user.id.toString(),
                type = WalletEventType.WALLET_BALANCE_CHANGED
            )
        )

        // and the new wallet is created in DB with balance 25.00
        val createdWallet = walletRepo.find(responseWallet.id)
        assertThat(createdWallet).isNotNull
        assertThat(createdWallet!!.balance).isEqualByComparingTo(BigDecimal("25.00"))
        assertThat(createdWallet.userId).isEqualTo(user.id)
        Unit
    }

    @Test
    fun `should create new wallet when only userId provided and no wallet exists`() = runBlocking {
        // given a user without any wallet
        val user = userRepo.create(User(id = 0u, name = "Charlie", age = 35))
        messageCollector.expectResult(count = 2)

        // when requesting topup by userId only
        val response = httpClient.get("/api/wallets/topup?amount=75.50&userId=${user.id}")

        // then HTTP 202 is returned with a newly created wallet having zero balance
        assertThat(response.status).isEqualTo(HttpStatusCode.Accepted)
        val responseWallet = response.body<Wallet>()
        assertThat(responseWallet.balance).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(responseWallet.userId).isEqualTo(user.id)

        // and both events are published with the auto-generated walletId
        val events = messageCollector.awaitMessages()
        val walletId = events.first().walletId
        assertThat(events).containsExactly(
            WalletEvent(
                walletId = walletId,
                amount = "75.50",
                userId = user.id.toString(),
                type = WalletEventType.WALLET_TOPUP_REQUESTED
            ),
            WalletEvent(
                walletId = walletId,
                amount = "75.50",
                userId = user.id.toString(),
                type = WalletEventType.WALLET_BALANCE_CHANGED
            )
        )

        // and a new wallet is created in DB with balance 75.50
        val createdWallet = walletRepo.find(responseWallet.id)
        assertThat(createdWallet).isNotNull
        assertThat(createdWallet!!.balance).isEqualByComparingTo(BigDecimal("75.50"))
        assertThat(createdWallet.userId).isEqualTo(user.id)
        Unit
    }

    @Test
    fun `should create new wallet when walletId not found but userId provided`() = runBlocking {
        // given a user and a non-existent walletId
        val user = userRepo.create(User(id = 0u, name = "Dave", age = 40))
        val nonExistentWalletId = 99999u
        messageCollector.expectResult(count = 2)

        // when requesting topup with walletId and userId
        val response =
            httpClient.get("/api/wallets/topup?amount=30.00&walletId=$nonExistentWalletId&userId=${user.id}")

        // then HTTP 202 is returned with a newly created wallet having zero balance
        assertThat(response.status).isEqualTo(HttpStatusCode.Accepted)
        val responseWallet = response.body<Wallet>()
        assertThat(responseWallet.balance).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(responseWallet.userId).isEqualTo(user.id)

        // and both events are published with the auto-generated walletId
        val events = messageCollector.awaitMessages()
        val walletId = events.first().walletId
        assertThat(events).containsExactly(
            WalletEvent(
                walletId = walletId,
                amount = "30.00",
                userId = user.id.toString(),
                type = WalletEventType.WALLET_TOPUP_REQUESTED
            ),
            WalletEvent(
                walletId = walletId,
                amount = "30.00",
                userId = user.id.toString(),
                type = WalletEventType.WALLET_BALANCE_CHANGED
            )
        )

        // and a new wallet is created in DB with balance 30.00
        val createdWallet = walletRepo.find(responseWallet.id)
        assertThat(createdWallet).isNotNull
        assertThat(createdWallet!!.balance).isEqualByComparingTo(BigDecimal("30.00"))
        assertThat(createdWallet.userId).isEqualTo(user.id)
        Unit
    }

    @Test
    fun `should return 422 when neither walletId nor userId provided`() = runTest {
        // given a request with only amount

        // when requesting topup without walletId or userId
        val response = httpClient.get("/api/wallets/topup?amount=10.00")

        // then HTTP 422 is returned
        assertThat(response.status).isEqualTo(HttpStatusCode.UnprocessableEntity)
    }

    @Test
    fun `should return 422 when walletId not found and no userId provided`() = runTest {
        // given a request with non-existent walletId and no userId

        // when requesting topup with a missing walletId only
        val response = httpClient.get("/api/wallets/topup?amount=10.00&walletId=99999")

        // then HTTP 422 is returned
        assertThat(response.status).isEqualTo(HttpStatusCode.UnprocessableEntity)
    }

    @Test
    fun `should return 400 for invalid wallet id`() = runTest {
        // given a request with invalid walletId format

        // when requesting topup with walletId=abc
        val response = httpClient.get("/api/wallets/topup?amount=10.00&walletId=abc")

        // then HTTP 400 is returned
        assertThat(response.status).isEqualTo(HttpStatusCode.BadRequest)
    }

    @Test
    fun `should return 400 for invalid amount`() = runTest {
        // given a request with invalid amount format

        // when requesting topup with amount=xyz
        val response = httpClient.get("/api/wallets/topup?amount=xyz&walletId=1")

        // then HTTP 400 is returned
        assertThat(response.status).isEqualTo(HttpStatusCode.BadRequest)
    }

    @Test
    fun `should return wallet when found`() = runBlocking {
        // given an existing user and wallet
        val user = userRepo.create(User(id = 0u, name = "Eve", age = 28))
        val wallet = walletRepo.create(
            Wallet(id = 0u, userId = user.id, balance = BigDecimal("500.00"))
        )

        // when requesting wallet by id
        val response = httpClient.get("/api/wallets/${wallet.id}")

        // then HTTP 200 is returned with the wallet
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        val responseWallet = response.body<Wallet>()
        assertThat(responseWallet).isEqualTo(wallet)
        Unit
    }

    @Test
    fun `should return 404 when wallet not found`() = runTest {
        // given a non-existent wallet id

        // when requesting wallet by id
        val response = httpClient.get("/api/wallets/99999")

        // then HTTP 404 is returned
        assertThat(response.status).isEqualTo(HttpStatusCode.NotFound)
    }

}
