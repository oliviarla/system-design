package com.x.batchapp.repository

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Range
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
class NewsFeedRedisRepository(
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, Any>,
    @Value("\${batch.news-feed-cache.max-items:50}") private val maxItems: Long
) {
    private val newsfeedKey = "newsfeed:"

    /**
     * Get all newsfeed keys
     */
    fun getAllNewsfeedKeys(): Flux<String> {
        return reactiveRedisTemplate.keys(newsfeedKey + "*")
    }

    /**
     * Get the size of a newsfeed zset
     */
    fun getNewsfeedSize(username: String): Mono<Long> {
        return reactiveRedisTemplate.opsForZSet()
            .size(newsfeedKey + username)
    }

    /**
     * Trim newsfeed zset to keep only the top maxItems (most recent feeds)
     * Removes items from lowest to highest score, keeping the highest scores
     */
    fun trimNewsfeed(username: String): Mono<Long> {
        return getNewsfeedSize(username)
            .flatMap { size ->
                if (size > maxItems) {
                    // Remove items with lowest scores (oldest feeds)
                    // Keep only the top maxItems items
                    val removeCount = size - maxItems
                    reactiveRedisTemplate.opsForZSet()
                        .removeRange(newsfeedKey + username, Range.closed(0L, removeCount - 1))
                } else {
                    Mono.just(0L)
                }
            }
    }
}