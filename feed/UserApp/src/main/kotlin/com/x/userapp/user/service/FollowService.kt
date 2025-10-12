package com.x.userapp.user.service

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
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

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
                followRedisRepository.incrFollowCount(currentUsername, usernameToFollow)
                    .doOnError { error ->
                        logger.error(
                            "Failed to increment follow count in Redis for $currentUsername -> $usernameToFollow",
                            error
                        )
                    }
                    .onErrorResume { Mono.empty() }
            ).then(
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
                followRedisRepository.decrFollowCount(currentUsername, usernameToUnfollow)
                    .doOnError { error ->
                        logger.error(
                            "Failed to decrement follow count in Redis for $currentUsername -> $usernameToUnfollow",
                            error
                        )
                    }
                    .onErrorResume { Mono.empty() }
            )
            .then(
                kafkaProducer.sendMessage(TOPIC_USER_UNFOLLOW, "$currentUsername $usernameToUnfollow")
            )
    }

    fun getFollowingCount(username: String): Mono<Long> {
        return followRedisRepository.getFollowingCount(username)
            .onErrorResume { error ->
                logger.error("Failed to get following count from Redis for $username, falling back to ScyllaDB", error)
                followingByUserRepository.countByKeyUsername(username)
                    .flatMap {
                        followRedisRepository.setFollowingCount(username, it)
                            .thenReturn(it)
                    }
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
                followerByUserRepository.countByKeyUsername(username)
                    .flatMap {
                        followRedisRepository.setFollowerCount(username, it)
                            .thenReturn(it)
                    }
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
    }
}
