package com.x.userapp.user.service

import com.x.userapp.user.domain.FollowerByUser
import com.x.userapp.user.domain.FollowerKey
import com.x.userapp.user.domain.FollowingByUser
import com.x.userapp.user.domain.FollowingKey
import com.x.userapp.user.repository.FollowRedisRepository
import com.x.userapp.user.repository.FollowerByUserRepository
import com.x.userapp.user.repository.FollowingByUserRepository
import com.x.userapp.user.repository.KafkaProducer
import com.x.userapp.user.repository.UserRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

@Service
class FollowService(
    private val followingByUserRepository: FollowingByUserRepository,
    private val followerByUserRepository: FollowerByUserRepository,
    private val userRepository: UserRepository,
    private val followRedisRepository: FollowRedisRepository,
    private val kafkaProducer: KafkaProducer,
    private val localCacheManager: LocalCacheManager
) {
    fun getFollowings(username: String): Flux<String> {
        return followingByUserRepository.findAllByKeyUsername(username)
            .map { followingByUser -> followingByUser.key.followingUsername }
    }

    fun getFollowers(username: String): Flux<String> {
        return followerByUserRepository.findAllByKeyUsername(username)
            .map { followerByUser -> followerByUser.key.followerUsername }
    }

    fun follow(currentUsername: String, usernameToFollow: String): Mono<Void> {
        val lockKey = "followAction:$currentUsername:$usernameToFollow"

        return followRedisRepository.acquireLock(lockKey)
            .flatMap { lockAcquired ->
                if (!lockAcquired) {
                    // Another request is processing, reject duplicate
                    Mono.error(IllegalStateException("Follow request already in progress."))
                } else {
                    // Lock acquired - proceed with follow operation
                    checkIfAlreadyFollowing(currentUsername, usernameToFollow)
                        .flatMap { exists ->
                            if (exists) {
                                Mono.error(IllegalStateException("Already following."))
                            } else {
                                saveFollowData(currentUsername, usernameToFollow)
                            }
                        }
                        .doFinally {
                            // Always release lock when done (success or error)
                            followRedisRepository.releaseLock(lockKey).subscribe()
                        }
                }
            }
            .onErrorResume { error ->
                // Ensure lock is released even on error
                followRedisRepository.releaseLock(lockKey)
                    .then(Mono.error(error))
            }
    }

    private fun checkIfAlreadyFollowing(currentUsername: String, usernameToFollow: String): Mono<Boolean> {
        val followingKey = FollowingKey(currentUsername, usernameToFollow)
        val followerKey = FollowerKey(usernameToFollow, currentUsername)

        val existsInFollowing = followingByUserRepository.existsById(followingKey)
        val existsInFollower = followerByUserRepository.existsById(followerKey)

        return Mono.zip(existsInFollowing, existsInFollower)
            .map { (existsInFollowing, existsInFollower) ->
                existsInFollowing || existsInFollower
            }
    }

    private fun saveFollowData(currentUsername: String, usernameToFollow: String): Mono<Void> {
        val following = FollowingByUser(key = FollowingKey(currentUsername, usernameToFollow))
        val follower = FollowerByUser(key = FollowerKey(usernameToFollow, currentUsername))

        return followingByUserRepository.save(following)
            .flatMap { savedFollowing ->
                followerByUserRepository.save(follower)
                    .onErrorResume { error ->
                        // Rollback: delete the following record if follower save fails
                        followingByUserRepository.delete(savedFollowing)
                            .then(Mono.error(error))
                    }
            }.then(
                // Update following_count for current user and follower_count for target user
                Mono.zip(
                    userRepository.incrementFollowingCount(currentUsername)
                        .doOnError { error ->
                            logger.error(
                                "Failed to increment following count for $currentUsername",
                                error
                            )
                        }
                        .onErrorResume { Mono.just(false) },
                    userRepository.incrementFollowerCount(usernameToFollow)
                        .doOnError { error ->
                            logger.error(
                                "Failed to increment follower count for $usernameToFollow",
                                error
                            )
                        }
                        .onErrorResume { Mono.just(false) }
                ).then()
            ).then(
                kafkaProducer.sendMessage(TOPIC_USER_FOLLOW, "$currentUsername $usernameToFollow")
            )
    }

    fun unfollow(currentUsername: String, usernameToUnfollow: String): Mono<Void> {
        val lockKey = "unfollowAction:$currentUsername:$usernameToUnfollow"

        return followRedisRepository.acquireLock(lockKey)
            .flatMap { lockAcquired ->
                if (!lockAcquired) {
                    // Another request is processing, reject duplicate
                    Mono.error(IllegalStateException("Unfollow request already in progress."))
                } else {
                    // Lock acquired - proceed with unfollow operation
                    checkIfAlreadyFollowing(currentUsername, usernameToUnfollow)
                        .flatMap { exists ->
                            if (!exists) {
                                Mono.error(IllegalStateException("Not following."))
                            } else {
                                deleteFollowData(currentUsername, usernameToUnfollow)
                            }
                        }
                        .doFinally {
                            // Always release lock when done (success or error)
                            followRedisRepository.releaseLock(lockKey).subscribe()
                        }
                }
            }
            .onErrorResume { error ->
                // Ensure lock is released even on error
                followRedisRepository.releaseLock(lockKey)
                    .then(Mono.error(error))
            }
    }

    private fun deleteFollowData(currentUsername: String, usernameToUnfollow: String): Mono<Void> {
        val followingKey = FollowingKey(currentUsername, usernameToUnfollow)
        val followerKey = FollowerKey(usernameToUnfollow, currentUsername)
        val following = FollowingByUser(key = followingKey)

        return followingByUserRepository.deleteById(followingKey)
            .onErrorResume {
                Mono.error(RuntimeException("Failed to delete following record.", it))
            }
            .then(
                followerByUserRepository.deleteById(followerKey)
                    .onErrorResume { error ->
                        // Rollback: restore the following record if follower delete fails
                        followingByUserRepository.save(following)
                            .then(Mono.error(error))
                    }
            )
            .then(
                // Update following_count for current user and follower_count for target user
                Mono.zip(
                    userRepository.decrementFollowingCount(currentUsername)
                        .doOnError { error ->
                            logger.error(
                                "Failed to decrement following count for $currentUsername",
                                error
                            )
                        }
                        .onErrorResume { Mono.just(false) },
                    userRepository.decrementFollowerCount(usernameToUnfollow)
                        .doOnError { error ->
                            logger.error(
                                "Failed to decrement follower count for $usernameToUnfollow",
                                error
                            )
                        }
                        .onErrorResume { Mono.just(false) }
                ).then()
            )
            .then(
                kafkaProducer.sendMessage(TOPIC_USER_UNFOLLOW, "$currentUsername $usernameToUnfollow")
            )
    }

    fun getFollowingCount(username: String): Mono<Long> {
        return followRedisRepository.getFollowingCount(username)
            .doOnNext { count ->
                // Always update local cache when we get data from Redis
                localCacheManager.setFollowingCount(username, count)
            }
            .switchIfEmpty(
                // Redis cache miss - try to acquire lock and fetch from DB
                fetchFollowingCountFromDBWithLock(username)
            )
            .onErrorResume { error ->
                // Redis error (circuit breaker) - fallback to local cache
                logger.error("Failed to get following count from Redis for $username, using local cache fallback", error)
                val localCount = localCacheManager.getFollowingCount(username)
                if (localCount != null) {
                    Mono.just(localCount)
                } else {
                    // Local cache miss - fetch from DB with lock to prevent thundering herd
                    logger.warn("Local cache miss for $username following count, fetching from DB")
                    fetchFollowingCountFromDBWithLock(username)
                }
            }
    }

    private fun fetchFollowingCountFromDBWithLock(username: String): Mono<Long> {
        val lockKey = "following:$username"

        return followRedisRepository.acquireLock(lockKey)
            .flatMap { lockAcquired ->
                if (lockAcquired) {
                    // Lock acquired - fetch from DB and update caches
                    followingByUserRepository.countByKeyUsername(username)
                        .flatMap { count ->
                            // Update both Redis and local cache
                            followRedisRepository.setFollowingCount(username, count)
                                .doOnSuccess { localCacheManager.setFollowingCount(username, count) }
                                .doFinally { followRedisRepository.releaseLock(lockKey).subscribe() }
                                .thenReturn(count)
                        }
                        .onErrorResume { dbError ->
                            // DB error - release lock and check local cache
                            logger.error("Failed to fetch following count from DB for $username", dbError)
                            followRedisRepository.releaseLock(lockKey)
                                .then(Mono.defer {
                                    val localCount = localCacheManager.getFollowingCount(username)
                                    if (localCount != null) {
                                        Mono.just(localCount)
                                    } else {
                                        Mono.error(dbError)
                                    }
                                })
                        }
                } else {
                    // Lock not acquired - another request is fetching, wait and retry from Redis or use local cache
                    Mono.delay(java.time.Duration.ofMillis(50))
                        .flatMap {
                            followRedisRepository.getFollowingCount(username)
                                .doOnNext { count -> localCacheManager.setFollowingCount(username, count) }
                                .switchIfEmpty(
                                    Mono.defer {
                                        val localCount = localCacheManager.getFollowingCount(username)
                                        if (localCount != null) {
                                            Mono.just(localCount)
                                        } else {
                                            // Still no data, return 0 as fallback
                                            logger.warn("Unable to fetch following count for $username, returning 0")
                                            Mono.just(0L)
                                        }
                                    }
                                )
                        }
                        .onErrorResume {
                            // If retry fails, use local cache
                            val localCount = localCacheManager.getFollowingCount(username)
                            if (localCount != null) {
                                Mono.just(localCount)
                            } else {
                                logger.warn("Unable to fetch following count for $username after lock wait, returning 0")
                                Mono.just(0L)
                            }
                        }
                }
            }
            .onErrorResume { lockError ->
                // Lock acquisition error - fallback to local cache
                logger.error("Failed to acquire lock for $username following count", lockError)
                val localCount = localCacheManager.getFollowingCount(username)
                if (localCount != null) {
                    Mono.just(localCount)
                } else {
                    logger.warn("No fallback available for $username following count, returning 0")
                    Mono.just(0L)
                }
            }
    }

    fun getFollowerCount(username: String): Mono<Long> {
        return followRedisRepository.getFollowerCount(username)
            .doOnNext { count ->
                // Always update local cache when we get data from Redis
                localCacheManager.setFollowerCount(username, count)
            }
            .switchIfEmpty(
                // Redis cache miss - try to acquire lock and fetch from DB
                fetchFollowerCountFromDBWithLock(username)
            )
            .onErrorResume { error ->
                // Redis error (circuit breaker) - fallback to local cache
                logger.error("Failed to get follower count from Redis for $username, using local cache fallback", error)
                val localCount = localCacheManager.getFollowerCount(username)
                if (localCount != null) {
                    Mono.just(localCount)
                } else {
                    // Local cache miss - fetch from DB with lock to prevent thundering herd
                    logger.warn("Local cache miss for $username follower count, fetching from DB")
                    fetchFollowerCountFromDBWithLock(username)
                }
            }
    }

    private fun fetchFollowerCountFromDBWithLock(username: String): Mono<Long> {
        val lockKey = "follower:$username"

        return followRedisRepository.acquireLock(lockKey)
            .flatMap { lockAcquired ->
                if (lockAcquired) {
                    // Lock acquired - fetch from DB and update caches
                    followerByUserRepository.countByKeyUsername(username)
                        .flatMap { count ->
                            // Update both Redis and local cache
                            followRedisRepository.setFollowerCount(username, count)
                                .doOnSuccess { localCacheManager.setFollowerCount(username, count) }
                                .doFinally { followRedisRepository.releaseLock(lockKey).subscribe() }
                                .thenReturn(count)
                        }
                        .onErrorResume { dbError ->
                            // DB error - release lock and check local cache
                            logger.error("Failed to fetch follower count from DB for $username", dbError)
                            followRedisRepository.releaseLock(lockKey)
                                .then(Mono.defer {
                                    val localCount = localCacheManager.getFollowerCount(username)
                                    if (localCount != null) {
                                        Mono.just(localCount)
                                    } else {
                                        Mono.error(dbError)
                                    }
                                })
                        }
                } else {
                    // Lock not acquired - another request is fetching, wait and retry from Redis or use local cache
                    Mono.delay(java.time.Duration.ofMillis(50))
                        .flatMap {
                            followRedisRepository.getFollowerCount(username)
                                .doOnNext { count -> localCacheManager.setFollowerCount(username, count) }
                                .switchIfEmpty(
                                    Mono.defer {
                                        val localCount = localCacheManager.getFollowerCount(username)
                                        if (localCount != null) {
                                            Mono.just(localCount)
                                        } else {
                                            // Still no data, return 0 as fallback
                                            logger.warn("Unable to fetch follower count for $username, returning 0")
                                            Mono.just(0L)
                                        }
                                    }
                                )
                        }
                        .onErrorResume {
                            // If retry fails, use local cache
                            val localCount = localCacheManager.getFollowerCount(username)
                            if (localCount != null) {
                                Mono.just(localCount)
                            } else {
                                logger.warn("Unable to fetch follower count for $username after lock wait, returning 0")
                                Mono.just(0L)
                            }
                        }
                }
            }
            .onErrorResume { lockError ->
                // Lock acquisition error - fallback to local cache
                logger.error("Failed to acquire lock for $username follower count", lockError)
                val localCount = localCacheManager.getFollowerCount(username)
                if (localCount != null) {
                    Mono.just(localCount)
                } else {
                    logger.warn("No fallback available for $username follower count, returning 0")
                    Mono.just(0L)
                }
            }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(FollowService::class.java)
        const val TOPIC_USER_FOLLOW = "user-follow"
        const val TOPIC_USER_UNFOLLOW = "user-unfollow"
    }
}
