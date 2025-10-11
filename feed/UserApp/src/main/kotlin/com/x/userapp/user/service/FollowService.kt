package com.x.userapp.user.service

import com.x.userapp.user.domain.FollowerByUser
import com.x.userapp.user.domain.FollowerKey
import com.x.userapp.user.domain.FollowingByUser
import com.x.userapp.user.domain.FollowingKey
import com.x.userapp.user.repository.FollowRedisRepository
import com.x.userapp.user.repository.FollowerByUserRepository
import com.x.userapp.user.repository.FollowingByUserRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

@Service
class FollowService(
    private val followingByUserRepository: FollowingByUserRepository,
    private val followerByUserRepository: FollowerByUserRepository,
    private val followRedisRepository: FollowRedisRepository
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
            }
            .flatMap { savedFollower ->
                followRedisRepository.incrFollowCount(currentUsername, usernameToFollow)
                    .onErrorResume { error ->
                        // Rollback: delete both records if Redis increment fails
                        followingByUserRepository.delete(following)
                            .then(followerByUserRepository.delete(savedFollower))
                            .then(Mono.error(error))
                    }
            }
            .then()
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
        val follower = FollowerByUser(key = followerKey)

        return followingByUserRepository.deleteById(followingKey)
            .flatMap {
                followerByUserRepository.deleteById(followerKey)
                    .onErrorResume { error ->
                        // Rollback: restore the following record if follower delete fails
                        followingByUserRepository.save(following)
                            .then(Mono.error(error))
                    }
            }
            .flatMap {
                followRedisRepository.decrFollowCount(currentUsername, usernameToUnfollow)
                    .onErrorResume { error ->
                        // Rollback: restore both records if Redis decrement fails
                        followingByUserRepository.save(following)
                            .then(followerByUserRepository.save(follower))
                            .then(Mono.error(error))
                    }
            }
            .then()
    }
}
