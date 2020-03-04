package test.bank.account

import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import test.bank.AccountChangedException
import test.bank.BusinessException
import test.bank.NotEnoughMoneyException
import test.responce.JsonItemBuilder
import test.responce.JsonItemResponse
import java.lang.Thread.sleep
import java.math.BigDecimal
import java.util.*
import kotlin.properties.Delegates

object MoneyTransferService {

    private val logger: Logger = LoggerFactory.getLogger("MoneyTransfer")

    private var sleepAfterExceptionMs by Delegates.notNull<Long>()
    private var repeatTimes by Delegates.notNull<Int>()

    fun setUp(sleepAfterExceptionMs: Long, repeatTimes: Int) {
        this.sleepAfterExceptionMs = sleepAfterExceptionMs
        this.repeatTimes = repeatTimes
    }

    fun getAllUsers() = transaction {
        JsonItemBuilder.success(UserAccount.all().map { it.toDto() })
    }

    fun transfer(request: MoneyTransaction): JsonItemResponse<MoneyTransaction> {
        logger.info("Request for money transfer ${request.amount} from ${request.from} to ${request.to}")

        repeat(repeatTimes) { num ->
            try {
                transaction {
                    if (request.from == request.to) throw BusinessException("It is the same account")
                    val users = UserAccount.forIds(listOf(request.from, request.to))
                        .map { it.id.value to it }.toMap()
                    val userFrom = users[request.from]
                        ?: throw RuntimeException("User with id = ${request.from} has not been found")
                    val userTo = users[request.to]
                        ?: throw RuntimeException("User with id = ${request.to} has not been found")

                    if (userFrom.amount < request.amount) throw NotEnoughMoneyException(userFrom.id.value)

                    changeAmount(user = userFrom, changeTo = -request.amount)
                    changeAmount(user = userTo, changeTo = request.amount)

                    logger.info("Money ${request.amount} from ${userFrom.id} to ${userTo.id} has been transferred successful")
                }

                return JsonItemBuilder.success(request)

            } catch (e: BusinessException) {
                return JsonItemBuilder.error(data = request, exception = e)
            } catch (e: AccountChangedException) {
                sleep(sleepAfterExceptionMs)
                logger.info("${e.message}. Try again ${num + 1} ...")
            } catch (e: NotEnoughMoneyException) {
                return JsonItemBuilder.error(data = request, exception = e)
            } catch (e: RuntimeException) {
                logger.error("Something wrong. Try again later", e)
                return JsonItemBuilder.error(data = request, exception = e, includeStackTrace = true)
            }
        }

        return JsonItemBuilder.error(data = request, exception = BusinessException("Something wrong. Try again later"))
    }

    private fun changeAmount(user: UserAccount, changeTo: BigDecimal) {
        val isUpdated = UserAccounts.update({
            UserAccounts.id eq user.id
            UserAccounts.version eq user.version
        }) {
            it[amount] = user.amount + changeTo
            it[version] = UUID.randomUUID()
        }
        if (isUpdated == 0) throw AccountChangedException(user.id.value)
    }

}
