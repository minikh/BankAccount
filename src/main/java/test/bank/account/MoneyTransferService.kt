package test.bank.account

import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import test.bank.BusinessException
import test.responce.JsonItemBuilder
import test.responce.JsonItemResponse
import java.lang.Thread.sleep
import java.math.BigDecimal
import java.util.*

val logger: Logger = LoggerFactory.getLogger("MoneyTransfer")
const val REPEAT_TIMES = 100
const val SLEEP_AFTER_EXCEPTION_MS = 100L

fun transfer(request: MoneyTransaction): JsonItemResponse<MoneyTransaction> {
    logger.info("Request for money transfer ${request.amount} from ${request.from} to ${request.to}")

    repeat(REPEAT_TIMES) { num ->
        try {
            transaction {
                val users = UserAccount.forIds(listOf(request.from, request.to))
                    .map { it.id.value to it }.toMap()
                val userFrom = users[request.from]
                    ?: throw RuntimeException("User with id = ${request.from} has not been found")
                val userTo = users[request.to]
                    ?: throw RuntimeException("User with id = ${request.to} has not been found")

                updateUser(user = userFrom, changeTo = -request.amount)
                updateUser(user = userTo, changeTo = request.amount)

                logger.info("Money ${request.amount} from ${userFrom.id} to ${userTo.id} has been transferred successful")
            }
            return JsonItemBuilder.success(request)
        } catch (e: BusinessException) {
            sleep(SLEEP_AFTER_EXCEPTION_MS)
            logger.info("${e.message}. Try again ${num + 1} ...")
        } catch (e: RuntimeException) {
            logger.error("Something wrong. Try again later", e)
            return JsonItemBuilder.error(data = request, exception = e, includeStackTrace = true)
        }
    }

    return JsonItemBuilder.error(data = request, exception = BusinessException("Something wrong. Try again later"))
}

private fun updateUser(user: UserAccount, changeTo: BigDecimal) {
    val isUpdated = UserAccounts.update({
        UserAccounts.id eq user.id
        UserAccounts.version eq user.version
    }) {
        it[amount] = user.amount + changeTo
        it[version] = UUID.randomUUID()
    }
    if (isUpdated == 0) throw BusinessException("User account has already been changed")
}
