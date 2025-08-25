package com.x.feedapp.feed.service

import com.x.feedapp.config.SnowflakeWebClientFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant

@Service
class IdGenerationService(
    private val webClientFactory: SnowflakeWebClientFactory
) {
    fun generateId(): Mono<String> {
        val client = webClientFactory.getClient()

        return client.get()
            .uri("/generate")
            .accept(MediaType.TEXT_PLAIN)
            .retrieve()
            .bodyToMono(String::class.java)
    }

    /**
     * Generates a minimum snowflake ID based on the provided instant.
     */
    fun generateMinSnowflakeId(instant: Instant): Mono<String> {
        val client = webClientFactory.getClient()

        return client.get()
            .uri("/generate")
            .accept(MediaType.TEXT_PLAIN)
            .retrieve()
            .bodyToMono(String::class.java)
    }

    fun generateMaxSnowflakeId(instant: Instant): Mono<String> {
        val client = webClientFactory.getClient()

        return client.get()
            .uri("/generate")
            .accept(MediaType.TEXT_PLAIN)
            .retrieve()
            .bodyToMono(String::class.java)
    }
}
