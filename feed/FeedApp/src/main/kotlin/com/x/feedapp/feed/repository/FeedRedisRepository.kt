package com.x.feedapp.feed.repository

import org.springframework.data.domain.Range
import org.springframework.data.redis.connection.Limit
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ZSetOperations
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
class FeedRedisRepository(private val reactiveRedisTemplate: ReactiveRedisTemplate<String, Any>) {

    private val key = "newsfeed:"
    private val defaultPageSize = 30

    /**
     *  Get news feed ids of news feed after the given last feed id from redis zset.
     */
    fun getNewsFeedIds(username: String, size: Int, lastFeedId: String): Flux<Long> {
        return reactiveRedisTemplate.opsForZSet()
            .reverseRangeByScore(key + username,
                Range.rightUnbounded(Range.Bound.inclusive(lastFeedId.toDouble())),
                Limit.limit().count(defaultPageSize))
            .mapNotNull { feedId -> feedId as? Long }
    }

    fun saveNewsFeedIds(username: String, feedCreator: String, feedIds: List<String>): Mono<Long> {
        val tuples: Collection<ZSetOperations.TypedTuple<Any>> =
            feedIds.mapNotNull { feedId ->
                val score = feedId.toLongOrNull()?.toDouble() ?: return@mapNotNull null
                ZSetOperations.TypedTuple.of(feedCreator, score)
            }
        if (tuples.isEmpty()) return Mono.just(0L)
        return reactiveRedisTemplate.opsForZSet().addAll(key + username, tuples)
    }

    fun getNewsFeedSize(username: String): Mono<Long> {
        return reactiveRedisTemplate.opsForZSet().size(key + username);
    }

}
