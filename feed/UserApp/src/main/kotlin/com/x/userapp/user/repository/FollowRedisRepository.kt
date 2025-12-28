package com.x.userapp.user.repository

import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Duration

@Repository
class FollowRedisRepository(private val redisTemplate: ReactiveRedisTemplate<String, Any>) {

    companion object {
        private val TTL_FOR_FOLLOW_COUNT = Duration.ofMinutes(5)
        private val LOCK_TTL = Duration.ofSeconds(10)
        private const val LOCK_KEY_PREFIX = "lock:followCount:"
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

    /**
     * Acquires a distributed lock using SETNX with TTL
     * Returns true if lock was acquired, false otherwise
     */
    fun acquireLock(lockKey: String): Mono<Boolean> {
        return redisTemplate.opsForValue()
            .setIfAbsent("$LOCK_KEY_PREFIX$lockKey", "locked", LOCK_TTL)
            .defaultIfEmpty(false)
    }

    /**
     * Releases the distributed lock
     */
    fun releaseLock(lockKey: String): Mono<Boolean> {
        return redisTemplate.opsForValue()
            .delete("$LOCK_KEY_PREFIX$lockKey")
    }
}
