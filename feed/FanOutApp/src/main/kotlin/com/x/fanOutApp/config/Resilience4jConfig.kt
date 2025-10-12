package com.x.fanOutApp.config

import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class Resilience4jConfig {

    @Bean
    fun redisRetry(): Retry {
        val config = RetryConfig.custom<Any>()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(500))
            .build()

        return Retry.of("redis-retry", config)
    }
}