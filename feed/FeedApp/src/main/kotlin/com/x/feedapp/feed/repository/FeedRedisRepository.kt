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
        var count = size
        if (size !in 0..defaultPageSize) {
            count = defaultPageSize
        }

        val range =
            if (lastFeedId.isEmpty()) Range.unbounded()
            else Range.rightUnbounded(Range.Bound.inclusive(lastFeedId.toDouble()))
        return reactiveRedisTemplate.opsForZSet()
            .reverseRangeByScoreWithScores(
                key + username,
                range,
                Limit.limit().count(count)
            )
            .mapNotNull { tuple -> tuple.score?.toLong() }
    }

    fun saveNewsFeedIds(username: String, feedCreator: String, feedIds: List<String>): Mono<Long> {
        val tuples: Collection<ZSetOperations.TypedTuple<Any>> =
            feedIds.map { feedId ->
                val score = feedId.toLong().toDouble()
                val member = feedCreator + feedId
                ZSetOperations.TypedTuple.of(member, score)
            }
        if (tuples.isEmpty()) return Mono.just(0L)
        return reactiveRedisTemplate.opsForZSet().addAll(key + username, tuples)
    }

    fun getNewsFeedSize(username: String): Mono<Long> {
        return reactiveRedisTemplate.opsForZSet().size(key + username);
    }

}
