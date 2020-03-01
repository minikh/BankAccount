package test.bank.account

import java.math.BigDecimal

data class MoneyTransaction(
    val from: Long,
    val to: Long,
    val amount: BigDecimal
)

data class UserAccountDto(
    val id: Long,
    val name: String,
    val amount: BigDecimal
)

fun UserAccount.toDto(): UserAccountDto {
    return UserAccountDto(
        id = id.value,
        name = name,
        amount = amount
    )
}
