package test.responce

import java.util.*

enum class ResponseStatus(val status: String) {
    SUCCESS("success"),
    ERROR("error"),
}

//@JsonInclude(JsonInclude.Include.NON_NULL)
data class JsonItemResponse<T>(
    val status: ResponseStatus,
    val message: String? = null,
    val code: String = UUID.randomUUID().toString(),
    val stackTrace: String? = null,
    val data: T? = null
)
