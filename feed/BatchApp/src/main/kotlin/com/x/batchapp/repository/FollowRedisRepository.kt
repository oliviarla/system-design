package com.x.batchapp.repository

import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
class FollowRedisRepository(
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, Any>
) {
    private val followingCountKey = "following:count:"
    private val followerCountKey = "follower:count:"

    fun getFollowingCount(username: String): Mono<Long> {
        return reactiveRedisTemplate.opsForValue()
            .get(followingCountKey + username)
            .map { it.toString().toLong() }
            .defaultIfEmpty(0L)
    }

    fun getFollowerCount(username: String): Mono<Long> {
        return reactiveRedisTemplate.opsForValue()
            .get(followerCountKey + username)
            .map { it.toString().toLong() }
            .defaultIfEmpty(0L)
    }

    fun setFollowingCount(username: String, count: Long): Mono<Boolean> {
        return reactiveRedisTemplate.opsForValue()
            .set(followingCountKey + username, count.toString())
    }

    fun setFollowerCount(username: String, count: Long): Mono<Boolean> {
        return reactiveRedisTemplate.opsForValue()
            .set(followerCountKey + username, count.toString())
    }

    fun getAllFollowingCountKeys(): Flux<String> {
        return reactiveRedisTemplate.keys(followingCountKey + "*")
    }

    fun getAllFollowerCountKeys(): Flux<String> {
        return reactiveRedisTemplate.keys(followerCountKey + "*")
    }

    fun getAllFollowCountUsernames(): Flux<String> {
        return Flux.merge(
            getAllFollowingCountKeys().map { it.removePrefix(followingCountKey) },
            getAllFollowerCountKeys().map { it.removePrefix(followerCountKey) }
        ).distinct()
    }
}