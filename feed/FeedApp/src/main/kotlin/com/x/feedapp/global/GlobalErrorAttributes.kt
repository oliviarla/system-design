package com.x.feedapp.global

import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import java.time.LocalDateTime

@Component
class GlobalErrorAttributes : DefaultErrorAttributes() {
    override fun getErrorAttributes(
        request: ServerRequest,
        options: ErrorAttributeOptions
    ): Map<String, Any> {
        val error = getError(request)
        return mapOf(
            "message" to (error.message ?: "Unexpected error"),
            "timestamp" to LocalDateTime.now().toString(),
            "path" to request.path(),
            "status" to getHttpStatus(error).value()
        )
    }

    private fun getHttpStatus(error: Throwable): HttpStatus {
        return when (error) {
            is IllegalArgumentException -> HttpStatus.BAD_REQUEST
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }
    }
}
