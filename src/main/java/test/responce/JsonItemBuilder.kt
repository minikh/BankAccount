package test.responce

import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter


class JsonItemBuilder {
    companion object {
        private val LOG = LoggerFactory.getLogger(this::class.java.simpleName)

        fun <T> success(data: T): JsonItemResponse<T> = JsonItemResponse(ResponseStatus.SUCCESS, data = data)

        fun <T> error(exception: Exception, includeStackTrace: Boolean = false, data: T? = null): JsonItemResponse<T> {
            val stackTrace = if (includeStackTrace) getStackTrace(exception) else null

            val message = if (exception.message != null) {
                exception.message
            } else {
                exception.javaClass.simpleName
            }
            return JsonItemResponse(
                status = ResponseStatus.ERROR,
                message = message,
                stackTrace = stackTrace,
                data = data
            )
        }

        private fun getStackTrace(throwable: Throwable): String? {
            val sw = StringWriter()
            val pw = PrintWriter(sw, true)
            throwable.printStackTrace(pw)
            return sw.buffer.toString()
        }
    }
}
