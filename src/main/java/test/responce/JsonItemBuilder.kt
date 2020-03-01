package test.responce

import test.getLogger
import java.io.PrintWriter
import java.io.StringWriter


class JsonItemBuilder {
    companion object {
        private val LOG = getLogger()

        fun <T> success(data: T): JsonItemResponse<T> {
            val response =
                JsonItemResponse(
                    ResponseStatus.SUCCESS,
                    data = data
                )
            LOG.debug(response.toString())
            return response
        }

        fun <T> error(exception: Exception, includeStackTrace: Boolean = false, data: T? = null): JsonItemResponse<T> {
            val stackTrace = if (includeStackTrace) getStackTrace(exception) else null

            val message = if (exception.message != null) {
                exception.message
            } else {
                exception.javaClass.simpleName
            }
            val response = JsonItemResponse<T>(
                status = ResponseStatus.ERROR,
                message = message,
                stackTrace = stackTrace,
                data = data
            )
            LOG.info(response.toString())
            return response
        }

        private fun getStackTrace(throwable: Throwable): String? {
            val sw = StringWriter()
            val pw = PrintWriter(sw, true)
            throwable.printStackTrace(pw)
            return sw.buffer.toString()
        }
    }
}
