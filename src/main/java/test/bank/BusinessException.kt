package test.bank

class BusinessException(message: String) : RuntimeException(message)

class NotEnoughMoneyException(userId: Long) : RuntimeException("User $userId doesn't have enough money")
class AccountChangedException(userId: Long) : RuntimeException("User $userId account has already been changed")
