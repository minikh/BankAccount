package test.settings

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import test.bank.account.UserAccount
import test.bank.account.UserAccounts
import java.math.BigDecimal
import java.util.*

object DbSettings {
    private val db by lazy {
        Database.connect("jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1;", "org.h2.Driver")
    }

    fun createDb() {
        db
        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(UserAccounts)
        }
    }

    fun createTestUsers() {
        db
        transaction {
            addLogger(StdOutSqlLogger)

            (0..10).forEach { num ->
                UserAccount.new {
                    name = "User-$num"
                    amount = BigDecimal(1000)
                    version = UUID.randomUUID()
                }
            }
        }
    }
}
