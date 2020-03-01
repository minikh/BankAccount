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
import org.junit.jupiter.api.Test
import test.application.bankApplicationServer
import test.bank.utils.ApiUtils
import test.settings.DbSettings
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.Executors
import kotlin.concurrent.thread

internal class MoneyTransferServiceKtTest {

    @Test
    internal fun `Server should be alive`() {
        withTestApplication({ bankApplicationServer() }) {
            with(handleRequest(HttpMethod.Get, "/")) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("alive", response.content)
            }
        }
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

                val users = ApiUtils.getResponseAs(
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
                amount = BigDecimal(1000)
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
        repeat(1000) {
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
            assertEquals(BigDecimal(1000).setScale(2), UserAccount[userTo.id].amount)
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
        val userMap = mutableMapOf<Long, BigDecimal>()
        moneyTransactions.forEach {
            val userFrom =  userMap[it.from]
            if (userFrom == null) {
                userMap[it.from] = startAmount - it.amount
            } else {
                userMap[it.from] = userFrom - it.amount
            }

            val userTo =  userMap[it.to]
            if (userTo == null) {
                userMap[it.to] = startAmount + it.amount
            } else {
                userMap[it.to] = userTo + it.amount
            }
        }

        userMap.map {
            transaction {
                assertEquals(it.value.setScale(2), UserAccount[it.key].amount)
            }
        }
    }
}
