package com.x.userapp.user.repository

import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Duration

@Repository
class FollowRedisRepository(private val redisTemplate: ReactiveRedisTemplate<String, Any>) {

    companion object {
        private val TTL_FOR_FOLLOW_COUNT = Duration.ofDays(2)
    }

    fun setFollowingCount(username: String, count: Long): Mono<Boolean> {
        return redisTemplate.opsForValue()
            .set("user:followingCount:$username", count, TTL_FOR_FOLLOW_COUNT)
    }

    fun setFollowerCount(username: String, count: Long): Mono<Boolean> {
        return redisTemplate.opsForValue()
            .set("user:followerCount:$username", count, TTL_FOR_FOLLOW_COUNT)
    }

    fun getFollowingCount(username: String): Mono<Long> {
        return redisTemplate.opsForValue()["user:followingCount:$username"]
            .map { it.toString().toLong() }
    }

    fun getFollowerCount(username: String): Mono<Long> {
        return redisTemplate.opsForValue()["user:followerCount:$username"]
            .map { it.toString().toLong() }
    }
}
