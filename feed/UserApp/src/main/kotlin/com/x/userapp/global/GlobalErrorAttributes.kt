package com.x.userapp.global

import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.RedisSystemException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import java.net.SocketException
import java.time.LocalDateTime

@Component
class GlobalErrorAttributes : DefaultErrorAttributes() {
    override fun getErrorAttributes(
        request: ServerRequest,
        options: ErrorAttributeOptions
    ): Map<String, Any> {
        val error = getError(request)
        val rootCause = getRootCause(error)

        return mapOf(
            "message" to getUserFriendlyMessage(error, rootCause),
            "timestamp" to LocalDateTime.now().toString(),
            "path" to request.path(),
            "status" to getHttpStatus(error, rootCause).value()
        )
    }

    private fun getRootCause(error: Throwable): Throwable {
        var cause = error
        while (cause.cause != null && cause.cause != cause) {
            cause = cause.cause!!
        }
        return cause
    }

    private fun getUserFriendlyMessage(error: Throwable, rootCause: Throwable): String {
        return when {
            isRedisConnectionError(error) || isRedisConnectionError(rootCause) ->
                "Service temporarily unavailable due to cache service issues. Please try again later."
            error is IllegalArgumentException ->
                error.message ?: "Invalid request"
            else ->
                "An unexpected error occurred. Please try again later."
        }
    }

    private fun isRedisConnectionError(error: Throwable): Boolean {
        // Check exception types
        if (error is RedisConnectionFailureException ||
            error is RedisSystemException ||
            error is SocketException ||
            error is java.io.IOException ||
            error is java.net.ConnectException) {
            return true
        }

        // Check exception class name (for cases where we don't have direct access to the class)
        val className = error::class.java.name
        if (className.contains("Redis", ignoreCase = true)) {
            return true
        }

        // Check error messages
        val message = error.message ?: ""
        return message.contains("Connection reset", ignoreCase = true) ||
               message.contains("Connection refused", ignoreCase = true) ||
               message.contains("Redis", ignoreCase = true) ||
               message.contains("Unable to connect", ignoreCase = true) ||
               message.contains("SocketException", ignoreCase = true)
    }

    private fun getHttpStatus(error: Throwable, rootCause: Throwable): HttpStatus {
        return when {
            error is IllegalArgumentException -> HttpStatus.BAD_REQUEST
            isRedisConnectionError(error) || isRedisConnectionError(rootCause) -> HttpStatus.SERVICE_UNAVAILABLE
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }
    }
}
