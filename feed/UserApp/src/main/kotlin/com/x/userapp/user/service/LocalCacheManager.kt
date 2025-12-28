package com.x.userapp.user.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class LocalCacheManager {

    private val followingCountCache: Cache<String, Long> = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofMinutes(5))
        .build()

    private val followerCountCache: Cache<String, Long> = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofMinutes(5))
        .build()

    fun getFollowingCount(username: String): Long? {
        return followingCountCache.getIfPresent(username)
    }

    fun setFollowingCount(username: String, count: Long) {
        followingCountCache.put(username, count)
    }

    fun getFollowerCount(username: String): Long? {
        return followerCountCache.getIfPresent(username)
    }

    fun setFollowerCount(username: String, count: Long) {
        followerCountCache.put(username, count)
    }

    fun invalidateFollowingCount(username: String) {
        followingCountCache.invalidate(username)
    }

    fun invalidateFollowerCount(username: String) {
        followerCountCache.invalidate(username)
    }
}