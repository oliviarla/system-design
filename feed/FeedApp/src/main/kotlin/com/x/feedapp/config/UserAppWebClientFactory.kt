package com.x.feedapp.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class UserAppWebClientFactory(
    private val builder: WebClient.Builder,
    private val properties: UserAppProperties
) {
    fun getClient(): WebClient {
        return builder.baseUrl(properties.url).build()
    }
}

@Component
@ConfigurationProperties(prefix = "spring.user-app")
class UserAppProperties {
    lateinit var url: String
}