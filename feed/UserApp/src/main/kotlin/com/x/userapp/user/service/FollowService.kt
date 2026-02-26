package com.x.userapp.user.service

import com.datastax.oss.driver.api.core.servererrors.OverloadedException
import com.x.userapp.user.domain.FollowerByUser
import com.x.userapp.user.domain.FollowerKey
import com.x.userapp.user.domain.FollowingByUser
import com.x.userapp.user.domain.FollowingKey
import com.x.userapp.user.repository.FollowRedisRepository
import com.x.userapp.user.repository.FollowerByUserRepository
import com.x.userapp.user.repository.FollowingByUserRepository
import com.x.userapp.user.repository.KafkaProducer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.QueryTimeoutException
import org.springframework.dao.TransientDataAccessException
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.retry.Retry

@Service
class FollowService(
    private val followingByUserRepository: FollowingByUserRepository,
    private val followerByUserRepository: FollowerByUserRepository,
    private val followRedisRepository: FollowRedisRepository,
    private val kafkaProducer: KafkaProducer
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
        return checkIfAlreadyFollowing(currentUsername, usernameToFollow)
            .flatMap { exists ->
                if (exists) {
                    Mono.error(IllegalStateException("Already following."))
                } else {
                    saveFollowData(currentUsername, usernameToFollow)
                }
            }
    }

    private fun checkIfAlreadyFollowing(currentUsername: String, usernameToFollow: String): Mono<Boolean> {
        return followingByUserRepository
            .existsById(FollowingKey(currentUsername, usernameToFollow))
    }

    private fun saveFollowData(currentUsername: String, usernameToFollow: String): Mono<Void> {
        return followingByUserRepository
            .save(FollowingByUser(key = FollowingKey(currentUsername, usernameToFollow)))
            .onErrorMap { error ->
                FollowOperationException(
                    "Failed to save following data for $currentUsername -> $usernameToFollow",
                    error
                )
            }
            .doOnNext { _ ->
                followerByUserRepository
                    .save(FollowerByUser(key = FollowerKey(usernameToFollow, currentUsername)))
                    .retryWhen(
                        Retry
                            .backoff(3, java.time.Duration.ofMillis(50))
                            .filter { ex ->
                                when (ex) {
                                    is OverloadedException,
                                    is QueryTimeoutException,
                                    is TransientDataAccessException -> true

                                    else -> false
                                }
                            }
                    )
                    .doOnError { error ->
                        val customError = FollowOperationException(
                            "Failed to save follower record after retries for $usernameToFollow <- $currentUsername",
                            error
                        )
                        logger.error(
                            "ALERT: Follower save failed after retries. Orphaned following record exists. Sending to DLQ for reconciliation.",
                            customError
                        )

                        // Send to dead-letter topic for reconciliation instead of attempting rollback
                        kafkaProducer.sendMessage(TOPIC_FOLLOW_FAILED, "$currentUsername $usernameToFollow")
                            .doOnError { kafkaError ->
                                logger.error(
                                    "CRITICAL: Failed to send to DLQ. Manual cleanup required for $currentUsername -> $usernameToFollow",
                                    kafkaError
                                )
                            }
                    }
            }.then(
                kafkaProducer.sendMessage(TOPIC_USER_FOLLOW, "$currentUsername $usernameToFollow")
            )
    }

    fun unfollow(currentUsername: String, usernameToUnfollow: String): Mono<Void> {
        return checkIfAlreadyFollowing(currentUsername, usernameToUnfollow)
            .flatMap { exists ->
                if (!exists) {
                    Mono.error(IllegalStateException("Not following."))
                } else {
                    deleteFollowData(currentUsername, usernameToUnfollow)
                }
            }
    }

    private fun deleteFollowData(currentUsername: String, usernameToUnfollow: String): Mono<Void> {
        val followingKey = FollowingKey(currentUsername, usernameToUnfollow)
        val followerKey = FollowerKey(usernameToUnfollow, currentUsername)

        return followingByUserRepository.deleteById(followingKey)
            .onErrorMap { error ->
                FollowOperationException(
                    "Failed to delete following data for $currentUsername -> $usernameToUnfollow",
                    error
                )
            }
            .doOnSuccess { _ ->
                followerByUserRepository.deleteById(followerKey)
                    .retryWhen(
                        Retry
                            .backoff(3, java.time.Duration.ofMillis(50))
                            .filter { ex ->
                                when (ex) {
                                    is OverloadedException,
                                    is QueryTimeoutException,
                                    is TransientDataAccessException -> true

                                    else -> false
                                }
                            }
                    )
                    .doOnError { error ->
                        val customError = FollowOperationException(
                            "Failed to delete follower record after retries for $usernameToUnfollow <- $currentUsername",
                            error
                        )
                        logger.error(
                            "ALERT: Follower delete failed after retries. Orphaned following record exists. Sending to DLQ for reconciliation.",
                            customError
                        )

                        kafkaProducer.sendMessage(TOPIC_UNFOLLOW_FAILED, "$currentUsername $usernameToUnfollow")
                            .doOnError { kafkaError ->
                                logger.error(
                                    "CRITICAL: Failed to send to DLQ. Manual cleanup required for $currentUsername -> $usernameToUnfollow",
                                    kafkaError
                                )
                            }
                    }
            }.then(
                kafkaProducer.sendMessage(TOPIC_USER_UNFOLLOW, "$currentUsername $usernameToUnfollow")
            )
    }

    fun getFollowingCount(username: String): Mono<Long> {
        return followRedisRepository.getFollowingCount(username)
            .onErrorResume { error ->
                logger.error("Failed to get following count from Redis for $username, falling back to ScyllaDB", error)
                return@onErrorResume Mono.empty()
            }
            .switchIfEmpty(
                followingByUserRepository.countByKeyUsername(username)
                    .flatMap {
                        followRedisRepository.setFollowingCount(username, it)
                            .thenReturn(it)
                    }
            )
    }

    fun getFollowerCount(username: String): Mono<Long> {
        return followRedisRepository.getFollowerCount(username)
            .onErrorResume { error ->
                logger.error("Failed to get follower count from Redis for $username, falling back to ScyllaDB", error)
                return@onErrorResume Mono.empty()
            }
            .switchIfEmpty(
                followerByUserRepository.countByKeyUsername(username)
                    .flatMap {
                        followRedisRepository.setFollowerCount(username, it)
                            .thenReturn(it)
                    }
            )
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(FollowService::class.java)
        const val TOPIC_USER_FOLLOW = "user-follow"
        const val TOPIC_USER_UNFOLLOW = "user-unfollow"
        const val TOPIC_FOLLOW_FAILED = "user-follow-failed"
        const val TOPIC_UNFOLLOW_FAILED = "user-unfollow-failed"
    }
}
