package com.x.userapp.user.repository

import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
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

    fun decrFollowCount(currentUsername: String, usernameToUnfollow: String): Mono<Void> {
        val script: RedisScript<Void> = RedisScript.of(
            """
            local followingKey = KEYS[1]
            local followerKey = KEYS[2]
            local ttl = ARGV[1]
            if redis.call("exists", followingKey) == 1 then
                redis.call("decr", followingKey)
                redis.call("expire", followingKey, ttl)
            end
            if redis.call("exists", followerKey) == 1 then
                redis.call("decr", followerKey)
                redis.call("expire", followerKey, ttl)
            end
            """.trimIndent(), Void::class.java
        )
        return redisTemplate.execute(
            script,
            listOf("user:followingCount:$currentUsername", "user:followerCount:$usernameToUnfollow"),
            TTL_FOR_FOLLOW_COUNT.seconds.toString()
        ).then()
    }

    fun incrFollowCount(currentUsername: String, usernameToFollow: String): Mono<Void> {
        val script: RedisScript<Void> = RedisScript.of(
            """
            local followingKey = KEYS[1]
            local followerKey = KEYS[2]
            local ttl = ARGV[1]
            if redis.call("exists", followingKey) == 1 then
                redis.call("incr", followingKey)
                redis.call("expire", followingKey, ttl)
            end
            if redis.call("exists", followerKey) == 1 then
                redis.call("incr", followerKey)
                redis.call("expire", followerKey, ttl)
            end
            """.trimIndent(), Void::class.java
        )
        return redisTemplate.execute(
            script,
            listOf("user:followingCount:$currentUsername", "user:followerCount:$usernameToFollow"),
            TTL_FOR_FOLLOW_COUNT.seconds.toString()
        ).then()
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
