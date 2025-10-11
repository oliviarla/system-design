package com.x.userapp.user.repository

import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import reactor.core.publisher.Mono

class FollowRedisRepository(private val redisTemplate: ReactiveRedisTemplate<String, Any>) {

    fun decrFollowCount(currentUsername: String, usernameToUnfollow: String): Mono<Void> {
        val script: RedisScript<Void> = RedisScript.of(
            """
            local followingKey = KEYS[1]
            local followerKey = KEYS[2]
            if redis.call("exists", followingKey) == 1 then
                redis.call("decr", followingKey)
            end
            if redis.call("exists", followerKey) == 1 then
                redis.call("decr", followerKey)
            end
            """.trimIndent(), Void::class.java
        )
        return redisTemplate.execute(
            script,
            listOf("user:followingCount:$currentUsername", "user:followerCount:$usernameToUnfollow"), "0"
        ).then()
    }

    fun incrFollowCount(currentUsername: String, usernameToFollow: String): Mono<Void> {
        val script: RedisScript<Void> = RedisScript.of(
            """
            local followingKey = KEYS[1]
            local followerKey = KEYS[2]
            if redis.call("exists", followingKey) == 1 then
                redis.call("incr", followingKey)
            end
            if redis.call("exists", followerKey) == 1 then
                redis.call("incr", followerKey)
            end
            """.trimIndent(), Void::class.java
        )
        return redisTemplate.execute(
            script,
            listOf("user:followingCount:$currentUsername", "user:followerCount:$usernameToFollow")
        ).then()
    }

}
