package test.bank.account

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object UserAccounts: LongIdTable() {
    val name = varchar("name", 50).index()
    val amount = decimal("amount", 100, 2)
    val version = uuid("version")
}

class UserAccount(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<UserAccount>(UserAccounts)

    var name by UserAccounts.name
    var amount by UserAccounts.amount
    var version by UserAccounts.version
}
