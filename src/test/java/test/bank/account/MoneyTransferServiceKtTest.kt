package test.bank.account

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import org.h2.util.MathUtils.randomInt
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import test.application.bankApplicationServer
import test.bank.utils.getResponseAs
import test.responce.ResponseStatus
import test.settings.DbSettings
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.Executors
import kotlin.concurrent.thread

internal class MoneyTransferServiceKtTest {

    @BeforeEach
    internal fun setUp() {
        MoneyTransferService.setUp(100, 100)
    }

    @Test
    internal fun `Should return all users`() {
        //given
        DbSettings.createDb()
        transaction {
            (0..10).map {
                UserAccount.new {
                    name = UUID.randomUUID().toString()
                    amount = BigDecimal(1000)
                    version = UUID.randomUUID()
                }
            }
        }

        //when
        withTestApplication({ bankApplicationServer() }) {
            with(handleRequest(HttpMethod.Get, "/users")) {
                //then
                assertEquals(HttpStatusCode.OK, response.status())

                val users = getResponseAs(
                    jacksonObjectMapper(), response.content, "$.data", Array<UserAccountDto>::class.java
                )
                assertTrue(users.isNotEmpty())
            }
        }
    }

    @Test
    internal fun `Should transfer money between two accounts`() {
        //given
        DbSettings.createDb()
        val userFrom = transaction {
            UserAccount.new {
                name = UUID.randomUUID().toString()
                amount = BigDecimal(100)
                version = UUID.randomUUID()
            }
        }
        val userTo = transaction {
            UserAccount.new {
                name = UUID.randomUUID().toString()
                amount = BigDecimal(0)
                version = UUID.randomUUID()
            }
        }
        val moneyTransaction =
            MoneyTransaction(
                from = userFrom.id.value,
                to = userTo.id.value,
                amount = BigDecimal(1)
            )

        //when
        val executor = Executors.newFixedThreadPool(10)
        repeat(100) {
            executor.execute(
                thread(start = false) {
                    withTestApplication({ bankApplicationServer() }) {
                        with(handleRequest(HttpMethod.Post, "/transfer") {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            setBody(jacksonObjectMapper().writeValueAsString(moneyTransaction))
                        }) {
                            //then
                            assertEquals(HttpStatusCode.OK, response.status())
                        }
                    }
                })
        }

        executor.shutdown()
        while (!executor.isTerminated) {
        }

        //then
        transaction {
            assertEquals(BigDecimal(0).setScale(2), UserAccount[userFrom.id].amount)
            assertEquals(BigDecimal(100).setScale(2), UserAccount[userTo.id].amount)
        }
    }

    @Test
    internal fun `Should transfer money between many different accounts`() {
        //given
        DbSettings.createDb()
        val startAmount = BigDecimal(1000)
        val users = transaction {
            addLogger(StdOutSqlLogger)

            (0..100).map {
                UserAccount.new {
                    name = UUID.randomUUID().toString()
                    amount = startAmount
                    version = UUID.randomUUID()
                }
            }
        }

        //when
        val executor = Executors.newFixedThreadPool(10)
        val moneyTransactions = (1..1000).mapNotNull {
            val userFrom = users[randomInt(users.size - 1)]
            val userTo = users[randomInt(users.size - 1)]
            if (userFrom == userTo) return@mapNotNull null

            val moneyTransaction =
                MoneyTransaction(
                    from = userFrom.id.value,
                    to = userTo.id.value,
                    amount = BigDecimal(1)
                )

            executor.execute(
                thread(start = false) {
                    withTestApplication({ bankApplicationServer() }) {
                        with(handleRequest(HttpMethod.Post, "/transfer") {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            setBody(jacksonObjectMapper().writeValueAsString(moneyTransaction))
                        }) {
                            //then
                            assertEquals(HttpStatusCode.OK, response.status())
                        }
                    }
                })
            return@mapNotNull moneyTransaction
        }

        executor.shutdown()
        while (!executor.isTerminated) {
        }

        //then
        val userAmounts = mutableMapOf<Long, BigDecimal>()
        moneyTransactions.forEach { transaction ->
            userAmounts.computeIfAbsent(transaction.from) { startAmount }
            userAmounts.computeIfPresent(transaction.from) { _, amount -> amount - transaction.amount }

            userAmounts.computeIfAbsent(transaction.to) { startAmount }
            userAmounts.computeIfPresent(transaction.to) { _, amount -> amount + transaction.amount }
        }

        userAmounts.map {
            transaction {
                assertEquals(it.value.setScale(2), UserAccount[it.key].amount)
            }
        }
    }

    @Test
    internal fun `Should throw exception if user doesn't have enough money`() {
        //given
        DbSettings.createDb()
        val userFrom = transaction {
            UserAccount.new {
                name = UUID.randomUUID().toString()
                amount = BigDecimal(0)
                version = UUID.randomUUID()
            }
        }
        val userTo = transaction {
            UserAccount.new {
                name = UUID.randomUUID().toString()
                amount = BigDecimal(0)
                version = UUID.randomUUID()
            }
        }
        val moneyTransaction =
            MoneyTransaction(
                from = userFrom.id.value,
                to = userTo.id.value,
                amount = BigDecimal(100)
            )

        //when
        withTestApplication({ bankApplicationServer() }) {
            with(handleRequest(HttpMethod.Post, "/transfer") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jacksonObjectMapper().writeValueAsString(moneyTransaction))
            }) {
                //then
                assertEquals(HttpStatusCode.OK, response.status())

                val status = getResponseAs(
                    jacksonObjectMapper(), response.content, "$.status", ResponseStatus::class.java
                )
                assertEquals(ResponseStatus.ERROR, status)

                val message = getResponseAs(
                    jacksonObjectMapper(), response.content, "$.message", String::class.java
                )
                assertEquals("User ${userFrom.id.value} doesn't have enough money", message)
            }
        }
    }

    @Test
    internal fun `Should throw exception if it is the same account`() {
        //given
        DbSettings.createDb()
        val user = transaction {
            UserAccount.new {
                name = UUID.randomUUID().toString()
                amount = BigDecimal(1000)
                version = UUID.randomUUID()
            }
        }
        val moneyTransaction =
            MoneyTransaction(
                from = user.id.value,
                to = user.id.value,
                amount = BigDecimal(100)
            )

        //when
        withTestApplication({ bankApplicationServer() }) {
            with(handleRequest(HttpMethod.Post, "/transfer") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jacksonObjectMapper().writeValueAsString(moneyTransaction))
            }) {
                //then
                assertEquals(HttpStatusCode.OK, response.status())

                val status = getResponseAs(
                    jacksonObjectMapper(), response.content, "$.status", ResponseStatus::class.java
                )
                assertEquals(ResponseStatus.ERROR, status)

                val message = getResponseAs(
                    jacksonObjectMapper(), response.content, "$.message", String::class.java
                )
                assertEquals("It is the same account", message)
            }
        }
    }
}
