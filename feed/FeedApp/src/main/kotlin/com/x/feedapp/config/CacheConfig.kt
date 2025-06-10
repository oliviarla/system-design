package com.x.feedapp.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class CacheConfig {

    @Bean
    fun reactiveRedisTemplate(
        connectionFactory: ReactiveRedisConnectionFactory
    ): ReactiveRedisTemplate<String, Any> {
        val serializationContext = RedisSerializationContext
            .newSerializationContext<String, Any>(StringRedisSerializer())
            .key(StringRedisSerializer())
            .value(GenericJackson2JsonRedisSerializer())
            .build()

        return ReactiveRedisTemplate(connectionFactory, serializationContext)
    }
}
