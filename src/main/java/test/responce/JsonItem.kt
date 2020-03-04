package test.responce

import java.util.*

enum class ResponseStatus {
    SUCCESS,
    ERROR,
}

data class JsonItemResponse<T>(
    val status: ResponseStatus,
    val message: String? = null,
    val code: String = UUID.randomUUID().toString(),
    val stackTrace: String? = null,
    val data: T? = null
)
