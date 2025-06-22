package com.x.feedapp.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import kotlin.random.Random

@Component
class SnowflakeWebClientFactory(
    private val builder: WebClient.Builder,
    private val properties: SnowflakeProperties
) {
    private val random = Random(System.currentTimeMillis())

    fun getClient(): WebClient {
        val urls = properties.urls
        check(urls.isNotEmpty()) { "Snowflake URLs must not be empty" }
        val baseUrl = urls[random.nextInt(urls.size)]
        return builder.baseUrl(baseUrl).build()
    }
}

@Component
@ConfigurationProperties(prefix = "spring.snowflake")
class SnowflakeProperties {
    lateinit var urls: List<String>
}
