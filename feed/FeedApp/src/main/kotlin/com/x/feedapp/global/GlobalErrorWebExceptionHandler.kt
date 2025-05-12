package com.x.feedapp.global

import org.springframework.boot.autoconfigure.web.WebProperties.Resources
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler
import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.reactive.error.ErrorAttributes
import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*

@Component
@Order(-2) // This should be ordered before the default error handler
class GlobalExceptionHandler(
    errorAttributes: GlobalErrorAttributes,
    applicationContext: ApplicationContext,
    serverCodecConfigurer: ServerCodecConfigurer,
) : AbstractErrorWebExceptionHandler(errorAttributes, Resources(), applicationContext) {
    init {
        this.setMessageWriters(serverCodecConfigurer.writers)
        this.setMessageReaders(serverCodecConfigurer.readers)
    }
    override fun getRoutingFunction(errorAttributes: ErrorAttributes): RouterFunction<ServerResponse> {
        return RouterFunctions.route(RequestPredicates.all()) { request ->
            val attrs = getErrorAttributes(request, ErrorAttributeOptions.defaults())
            val status = HttpStatus.valueOf(attrs["status"] as Int)
            ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(attrs)
        }
    }
}
