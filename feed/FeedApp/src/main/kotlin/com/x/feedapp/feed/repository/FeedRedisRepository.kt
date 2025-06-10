package com.x.feedapp.feed.repository

import org.springframework.data.domain.Range
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
class FeedRedisRepository(private val reactiveRedisTemplate: ReactiveRedisTemplate<String, Any>) {

    private val key = "newsfeed:"
    private val defaultPageSize = 30

    /**
     * ZSET
     * - Key: "newsfeed:{userId}"
     * - Score: timestamp
     * - Member: "{feedId}"
     *
     * 1. Cache에 Sorted Set 있는지 확인 -> 있다면 해당 ID들 반환
     * 2. Cache에 없으면 회원 ID의 팔로잉 ID 목록 조회
     * 3. 각 회원마다 최신 피드를 캐싱하고 있으므로 최신 5개씩 가져와 이를 조합한다.
     */
    fun getNewsFeedIds(userId: Long, page: Int): Flux<Long> {
        val start = page * defaultPageSize
        val end = start + defaultPageSize - 1

        return reactiveRedisTemplate.opsForZSet()
            .reverseRange(key + userId, Range.closed(start.toLong(), end.toLong()))
            .mapNotNull { feedId -> feedId as? Long }
    }

    fun getRecentFeedIds(userId: Flux<Long>): Flux<Long> {
        // get the latest 5 feeds of each user
//        return reactiveRedisTemplate.opsForZSet()
        return Flux.empty()
    }
}
